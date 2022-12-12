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
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.NoQuotaAvailable;
import com.redhat.service.smartevents.infra.v2.api.V2APIConstants;
import com.redhat.service.smartevents.infra.v2.api.models.ComponentType;
import com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus;
import com.redhat.service.smartevents.infra.v2.api.models.DefaultConditions;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.infra.v2.api.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.manager.v2.ams.QuotaConfigurationProvider;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.ProcessorResponse;
import com.redhat.service.smartevents.manager.v2.persistence.dao.ProcessorDAO;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Condition;
import com.redhat.service.smartevents.manager.v2.persistence.models.Operation;
import com.redhat.service.smartevents.manager.v2.persistence.models.Processor;

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

        // Processor, Connector and Work should always be created in the same transaction
        processorDAO.persist(processor);

        // TODO: schedule work for dependencies

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

        return processorResponse;
    }

    private String getBridgeHref(Processor processor) {
        if (Objects.nonNull(processor.getBridge())) {
            return V2APIConstants.V2_USER_API_BASE_PATH + processor.getBridge().getId() + "/processors/" + processor.getId();
        }
        return null;
    }

}
