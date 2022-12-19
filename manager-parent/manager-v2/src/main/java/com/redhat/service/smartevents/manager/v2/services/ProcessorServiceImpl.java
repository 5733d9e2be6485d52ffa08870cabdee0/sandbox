package com.redhat.service.smartevents.manager.v2.services;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.service.smartevents.infra.core.metrics.MetricsOperation;
import com.redhat.service.smartevents.infra.core.models.ListResult;
import com.redhat.service.smartevents.infra.v2.api.V2;
import com.redhat.service.smartevents.infra.v2.api.V2APIConstants;
import com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.user.AlreadyExistingItemException;
import com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.user.BadRequestException;
import com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.user.BridgeLifecycleException;
import com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.user.NoQuotaAvailable;
import com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.user.ProcessorLifecycleException;
import com.redhat.service.smartevents.infra.v2.api.models.ComponentType;
import com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus;
import com.redhat.service.smartevents.infra.v2.api.models.DefaultConditions;
import com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ResourceStatusDTO;
import com.redhat.service.smartevents.infra.v2.api.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.infra.v2.api.models.queries.QueryResourceInfo;
import com.redhat.service.smartevents.manager.core.workers.WorkManager;
import com.redhat.service.smartevents.manager.v2.ams.QuotaConfigurationProvider;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.ProcessorResponse;
import com.redhat.service.smartevents.manager.v2.metrics.ManagerMetricsServiceV2;
import com.redhat.service.smartevents.manager.v2.persistence.dao.ProcessorDAO;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Condition;
import com.redhat.service.smartevents.manager.v2.persistence.models.Operation;
import com.redhat.service.smartevents.manager.v2.persistence.models.Processor;
import com.redhat.service.smartevents.manager.v2.utils.ConditionUtilities;
import com.redhat.service.smartevents.manager.v2.utils.StatusUtilities;

import static com.redhat.service.smartevents.manager.v2.utils.StatusUtilities.getManagedResourceStatus;
import static com.redhat.service.smartevents.manager.v2.utils.StatusUtilities.getModifiedAt;
import static com.redhat.service.smartevents.manager.v2.utils.StatusUtilities.getStatusMessage;

@ApplicationScoped
public class ProcessorServiceImpl implements ProcessorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessorServiceImpl.class);

    @Inject
    ProcessorDAO processorDAO;

    @Inject
    BridgeService bridgeService;

    @Inject
    QuotaConfigurationProvider quotaConfigurationProvider;

    @V2
    @Inject
    WorkManager workManager;

    @Inject
    ManagerMetricsServiceV2 metricsService;

    @Override
    @Transactional
    public Processor getProcessor(String bridgeId, String processorId, String customerId) {
        Bridge bridge = bridgeService.getBridge(bridgeId, customerId);
        Processor processor = processorDAO.findByIdBridgeIdAndCustomerId(bridge.getId(), processorId, bridge.getCustomerId());
        if (Objects.isNull(processor)) {
            throw new ItemNotFoundException(String.format("Processor with id '%s' does not exist on Bridge '%s' for customer '%s'", processorId, bridgeId, customerId));
        }

        return processor;
    }

    @Override
    @Transactional
    public ListResult<Processor> getProcessors(String bridgeId, String customerId, QueryResourceInfo queryInfo) {
        Bridge bridge = bridgeService.getBridge(bridgeId, customerId);
        ManagedResourceStatusV2 status = StatusUtilities.getManagedResourceStatus(bridge);
        if (status != ManagedResourceStatusV2.READY && status != ManagedResourceStatusV2.FAILED) {
            throw new BridgeLifecycleException(String.format("Bridge with id '%s' for customer '%s' is not in READY/FAILED state.", bridge.getId(), bridge.getCustomerId()));
        }
        return processorDAO.findByBridgeIdAndCustomerId(bridgeId, customerId, queryInfo);
    }

    @Override
    @Transactional
    public Processor createProcessor(String bridgeId, String customerId, String owner, String organisationId, ProcessorRequest processorRequest) {
        // We cannot deploy Processors to a Bridge that is not available. This throws an Exception if the Bridge is not READY.
        Bridge bridge = bridgeService.getReadyBridge(bridgeId, customerId);

        // Check processors limits
        long totalProcessors = processorDAO.countByBridgeIdAndCustomerId(bridgeId, customerId);
        if (totalProcessors + 1 > quotaConfigurationProvider.getOrganisationQuotas(organisationId).getProcessorsQuota()) {
            throw new NoQuotaAvailable(
                    String.format("There are already '%d' processors attached to the bridge '%s': you reached the limit for your organisation settings.", totalProcessors, bridgeId));
        }

        return doCreateProcessor(bridge, customerId, owner, processorRequest);
    }

    private Processor doCreateProcessor(Bridge bridge, String customerId, String owner, ProcessorRequest processorRequest) {
        String bridgeId = bridge.getId();
        if (processorDAO.findByBridgeIdAndName(bridgeId, processorRequest.getName()) != null) {
            throw new AlreadyExistingItemException("Processor with name '" + processorRequest.getName() + "' already exists for bridge with id '" + bridgeId + "' for customer '" + customerId + "'");
        }

        Operation operation = new Operation();
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));
        operation.setType(OperationType.CREATE);

        Processor processor = processorRequest.toEntity();
        processor.setOperation(operation);
        processor.setConditions(createAcceptedConditions());
        processor.setOwner(owner);
        processor.setBridge(bridge);
        processor.setSubmittedAt(ZonedDateTime.now(ZoneOffset.UTC));
        processor.setGeneration(0);

        ObjectNode requestedFlows = processorRequest.getFlows();

        ProcessorDefinition definition = new ProcessorDefinition(requestedFlows);
        processor.setDefinition(definition);

        // Processor and Work should always be created in the same transaction
        processorDAO.persist(processor);
        workManager.schedule(processor);

        metricsService.onOperationStart(processor, MetricsOperation.MANAGER_RESOURCE_PROVISION);

        LOGGER.info("Processor with id '{}' for customer '{}' on bridge '{}' has been marked for creation",
                processor.getId(),
                processor.getBridge().getCustomerId(),
                processor.getBridge().getId());

        return processor;
    }

    private List<Condition> createAcceptedConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition(DefaultConditions.CP_CONTROL_PLANE_READY_NAME, ConditionStatus.UNKNOWN, null, null, null, ComponentType.MANAGER, ZonedDateTime.now(ZoneOffset.UTC)));
        conditions.add(new Condition(DefaultConditions.CP_DATA_PLANE_READY_NAME, ConditionStatus.UNKNOWN, null, null, null, ComponentType.SHARD, ZonedDateTime.now(ZoneOffset.UTC)));
        return conditions;
    }

    @Transactional
    @Override
    public Long getProcessorsCount(String bridgeId, String customerId) {
        return processorDAO.countByBridgeIdAndCustomerId(bridgeId, customerId);
    }

    @Override
    @Transactional
    public Processor updateProcessor(String bridgeId, String processorId, String customerId, ProcessorRequest processorRequest) {
        // Attempt to load the Bridge. We cannot update a Processor if the Bridge is not available.
        bridgeService.getReadyBridge(bridgeId, customerId);
        Processor existingProcessor = getProcessor(bridgeId, processorId, customerId);
        long nextGeneration = existingProcessor.getGeneration() + 1;

        return doUpdateProcessor(bridgeId,
                processorId,
                customerId,
                processorRequest,
                nextGeneration);
    }

    private Processor doUpdateProcessor(String bridgeId,
            String processorId,
            String customerId,
            ProcessorRequest processorRequest,
            long nextGeneration) {
        Processor existingProcessor = getProcessor(bridgeId, processorId, customerId);

        if (!StatusUtilities.isActionable(existingProcessor)) {
            throw new ProcessorLifecycleException(String.format("Processor with id '%s' for customer '%s' is not in an actionable state.",
                    processorId,
                    customerId));
        }
        ProcessorDefinition existingDefinition = existingProcessor.getDefinition();
        ObjectNode existingFlows = existingDefinition.getFlows();

        // Validate update.
        // Name cannot be updated.
        if (!Objects.equals(existingProcessor.getName(), processorRequest.getName())) {
            throw new BadRequestException("It is not possible to update the Processor's name.");
        }

        // Construct updated definition
        ObjectNode updatedFlows = processorRequest.getFlows();

        // No need to update CRD if the definition is unchanged
        if (Objects.equals(existingFlows, updatedFlows)) {
            return existingProcessor;
        }

        ProcessorDefinition updatedDefinition = new ProcessorDefinition(updatedFlows);

        // Create new definition copying existing properties
        Operation operation = new Operation();
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));
        operation.setType(OperationType.UPDATE);

        existingProcessor.setOperation(operation);
        existingProcessor.setConditions(createAcceptedConditions());
        existingProcessor.setDefinition(updatedDefinition);
        existingProcessor.setGeneration(nextGeneration);

        workManager.schedule(existingProcessor);

        metricsService.onOperationStart(existingProcessor, MetricsOperation.MANAGER_RESOURCE_UPDATE);

        LOGGER.info("Processor with id '{}' for customer '{}' on bridge '{}' has been marked for update",
                existingProcessor.getId(),
                existingProcessor.getBridge().getCustomerId(),
                existingProcessor.getBridge().getId());

        return existingProcessor;
    }

    @Override
    @Transactional
    public void deleteProcessor(String bridgeId, String processorId, String customerId) {
        Processor processor = processorDAO.findByIdBridgeIdAndCustomerId(bridgeId, processorId, customerId);
        if (Objects.isNull(processor)) {
            throw new ItemNotFoundException(String.format("Processor with id '%s' does not exist on bridge '%s' for customer '%s'", processorId, bridgeId, customerId));
        }
        if (!StatusUtilities.isActionable(processor)) {
            throw new ProcessorLifecycleException("Processor could only be deleted if its in READY/FAILED state.");
        }

        Operation operation = new Operation();
        operation.setRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));
        operation.setType(OperationType.DELETE);

        processor.setOperation(operation);
        processor.setConditions(createDeletedConditions());

        workManager.schedule(processor);

        metricsService.onOperationStart(processor, MetricsOperation.MANAGER_RESOURCE_DELETE);

        LOGGER.info("Processor with id '{}' for customer '{}' on bridge '{}' has been marked for deletion",
                processor.getId(),
                processor.getBridge().getCustomerId(),
                processor.getBridge().getId());
    }

    private List<Condition> createDeletedConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition(DefaultConditions.CP_CONTROL_PLANE_DELETED_NAME, ConditionStatus.UNKNOWN, null, null, null, ComponentType.MANAGER, ZonedDateTime.now(ZoneOffset.UTC)));
        conditions.add(new Condition(DefaultConditions.CP_DATA_PLANE_DELETED_NAME, ConditionStatus.UNKNOWN, null, null, null, ComponentType.SHARD, ZonedDateTime.now(ZoneOffset.UTC)));
        return conditions;
    }

    @Override
    public List<Processor> findByShardIdToDeployOrDelete(String shardId) {
        return processorDAO.findByShardIdToDeployOrDelete(shardId);
    }

    @Override
    @Transactional
    public Processor updateProcessorStatus(ResourceStatusDTO statusDTO) {
        Processor processor = processorDAO.findByIdWithConditions(statusDTO.getId());
        if (Objects.isNull(processor)) {
            throw new ItemNotFoundException(String.format("Processor with id '%s' does not exist.", statusDTO.getId()));
        }
        if (processor.getGeneration() != statusDTO.getGeneration()) {
            LOGGER.info("Update for Processor with id '{}' was discarded. The expected generation '{}' did not match the actual '{}'.",
                    processor.getId(),
                    processor.getGeneration(),
                    statusDTO.getGeneration());
            return processor;
        }

        Operation operation = processor.getOperation();
        List<Condition> conditions = processor.getConditions();
        // Set the updated conditions to the existing Manager conditions to begin; then copy in the new Operator conditions
        List<Condition> updatedConditions = conditions.stream().filter(c -> c.getComponent() == ComponentType.MANAGER).collect(Collectors.toList());
        statusDTO.getConditions().forEach(c -> updatedConditions.add(Condition.from(c, ComponentType.SHARD)));

        // Don't do anything if Conditions are unchanged from our previous state.
        ManagedResourceStatusV2 status = StatusUtilities.getManagedResourceStatus(operation, conditions);
        ManagedResourceStatusV2 originalStatus = StatusUtilities.getManagedResourceStatus(operation, updatedConditions);
        if (Objects.equals(status, originalStatus)) {
            LOGGER.info("Update for Processor with id '{}' was discarded. The ManagedResourceStatus reflected by the Conditions is unchanged.", processor.getId());
            return processor;
        }

        processor.setConditions(updatedConditions);

        switch (operation.getType()) {
            case CREATE:
                if (ConditionUtilities.isOperationComplete(updatedConditions)) {
                    if (Objects.isNull(processor.getPublishedAt())) {
                        processor.setPublishedAt(ZonedDateTime.now(ZoneOffset.UTC));
                        metricsService.onOperationComplete(processor, MetricsOperation.MANAGER_RESOURCE_PROVISION);
                    }
                } else if (ConditionUtilities.isOperationFailed(updatedConditions)) {
                    metricsService.onOperationFailed(processor, MetricsOperation.MANAGER_RESOURCE_PROVISION);
                }
                break;

            case UPDATE:
                if (ConditionUtilities.isOperationComplete(updatedConditions)) {
                    metricsService.onOperationComplete(processor, MetricsOperation.MANAGER_RESOURCE_UPDATE);
                } else if (ConditionUtilities.isOperationFailed(updatedConditions)) {
                    metricsService.onOperationFailed(processor, MetricsOperation.MANAGER_RESOURCE_UPDATE);
                }
                break;

            case DELETE:
                if (ConditionUtilities.isOperationComplete(updatedConditions)) {
                    // There is no need to check if the Processor exists as any subsequent Status Update cycle
                    // would not include the same Processor if it had been deleted. It would not have existed
                    // on the database and hence would not have been included in the Status Update cycle.
                    processorDAO.deleteById(statusDTO.getId());
                    metricsService.onOperationComplete(processor, MetricsOperation.MANAGER_RESOURCE_DELETE);
                } else if (ConditionUtilities.isOperationFailed(updatedConditions)) {
                    metricsService.onOperationFailed(processor, MetricsOperation.MANAGER_RESOURCE_DELETE);
                }
                break;
        }

        return processor;
    }

    @Override
    public ProcessorDTO toDTO(Processor processor) {
        ProcessorDTO dto = new ProcessorDTO();
        dto.setId(processor.getId());
        dto.setBridgeId(processor.getBridge().getId());
        dto.setCustomerId(processor.getBridge().getCustomerId());
        dto.setOwner(processor.getOwner());
        dto.setName(processor.getName());
        dto.setFlows(processor.getDefinition().getFlows());
        dto.setOperationType(processor.getOperation().getType());
        dto.setGeneration(processor.getGeneration());
        return dto;
    }

    @Override
    public ProcessorResponse toResponse(Processor processor) {
        ProcessorResponse processorResponse = new ProcessorResponse();

        processorResponse.setId(processor.getId());
        processorResponse.setName(processor.getName());
        processorResponse.setStatus(getManagedResourceStatus(processor));
        processorResponse.setPublishedAt(processor.getPublishedAt());
        processorResponse.setSubmittedAt(processor.getSubmittedAt());
        processorResponse.setModifiedAt(getModifiedAt(processor));
        processorResponse.setOwner(processor.getOwner());
        processorResponse.setHref(getBridgeHref(processor));
        processorResponse.setStatusMessage(getStatusMessage(processor));
        processorResponse.setFlows(processor.getDefinition().getFlows());

        return processorResponse;
    }

    private String getBridgeHref(Processor processor) {
        if (Objects.nonNull(processor.getBridge())) {
            return V2APIConstants.V2_USER_API_BASE_PATH + processor.getBridge().getId() + "/processors/" + processor.getId();
        }
        return null;
    }

}
