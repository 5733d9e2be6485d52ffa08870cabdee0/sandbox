package com.redhat.service.smartevents.manager.v2.services;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.AlreadyExistingItemException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.BadRequestException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.BridgeLifecycleException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.NoQuotaAvailable;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.ProcessorLifecycleException;
import com.redhat.service.smartevents.infra.core.models.ListResult;
import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.core.models.queries.QueryResourceInfo;
import com.redhat.service.smartevents.infra.v2.api.V2;
import com.redhat.service.smartevents.infra.v2.api.V2APIConstants;
import com.redhat.service.smartevents.infra.v2.api.models.ComponentType;
import com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus;
import com.redhat.service.smartevents.infra.v2.api.models.DefaultConditions;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.infra.v2.api.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.manager.core.workers.WorkManager;
import com.redhat.service.smartevents.manager.v2.ams.QuotaConfigurationProvider;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.ProcessorResponse;
import com.redhat.service.smartevents.manager.v2.persistence.dao.ProcessorDAO;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Condition;
import com.redhat.service.smartevents.manager.v2.persistence.models.Operation;
import com.redhat.service.smartevents.manager.v2.persistence.models.Processor;
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
        ManagedResourceStatus status = StatusUtilities.getManagedResourceStatus(bridge);
        if (status != ManagedResourceStatus.READY && status != ManagedResourceStatus.FAILED) {
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

    @Transactional
    @Override
    public Long getProcessorsCount(String bridgeId, String customerId) {
        return processorDAO.countByBridgeIdAndCustomerId(bridgeId, customerId);
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

        // TODO: record metrics with MetricsService

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

        // TODO: schedule work for dependencies

        // TODO: record metrics with MetricsService

        LOGGER.info("Processor with id '{}' for customer '{}' on bridge '{}' has been marked for update",
                existingProcessor.getId(),
                existingProcessor.getBridge().getCustomerId(),
                existingProcessor.getBridge().getId());

        return existingProcessor;
    }

    @Override
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

        // TODO: schedule work for dependencies

        // TODO: record metrics with MetricsService

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
