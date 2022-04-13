package com.redhat.service.smartevents.manager;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.infra.api.APIConstants;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.AlreadyExistingItemException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.BadRequestException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ProcessorLifecycleException;
import com.redhat.service.smartevents.infra.models.ListResult;
import com.redhat.service.smartevents.infra.models.QueryInfo;
import com.redhat.service.smartevents.infra.models.actions.Action;
import com.redhat.service.smartevents.infra.models.actions.Source;
import com.redhat.service.smartevents.infra.models.dto.KafkaConnectionDTO;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.models.filters.BaseFilter;
import com.redhat.service.smartevents.infra.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.manager.api.models.responses.ProcessorResponse;
import com.redhat.service.smartevents.manager.connectors.ConnectorsService;
import com.redhat.service.smartevents.manager.dao.ProcessorDAO;
import com.redhat.service.smartevents.manager.models.Bridge;
import com.redhat.service.smartevents.manager.models.Processor;
import com.redhat.service.smartevents.manager.providers.InternalKafkaConfigurationProvider;
import com.redhat.service.smartevents.manager.providers.ResourceNamesProvider;
import com.redhat.service.smartevents.manager.workers.WorkManager;
import com.redhat.service.smartevents.processor.GatewayConfigurator;
import com.redhat.service.smartevents.processor.actions.ActionResolver;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

@ApplicationScoped
public class ProcessorServiceImpl implements ProcessorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessorServiceImpl.class);

    @Inject
    ProcessorDAO processorDAO;

    @Inject
    BridgesService bridgesService;

    @Inject
    MeterRegistry meterRegistry;

    @Inject
    ObjectMapper mapper;

    @Inject
    GatewayConfigurator gatewayConfigurator;

    @Inject
    ConnectorsService connectorService;

    @Inject
    InternalKafkaConfigurationProvider internalKafkaConfigurationProvider;

    @Inject
    ResourceNamesProvider resourceNamesProvider;

    @Inject
    ShardService shardService;

    @Inject
    WorkManager workManager;

    @Transactional
    @Override
    public Processor getProcessor(String bridgeId, String processorId, String customerId) {

        Bridge bridge = bridgesService.getBridge(bridgeId, customerId);
        Processor processor = processorDAO.findByIdBridgeIdAndCustomerId(bridge.getId(), processorId, bridge.getCustomerId());
        if (processor == null) {
            throw new ItemNotFoundException(String.format("Processor with id '%s' does not exist on Bridge '%s' for customer '%s'", processorId, bridgeId, customerId));
        }

        return processor;
    }

    @Override
    @Transactional
    public Processor createProcessor(String bridgeId, String customerId, ProcessorRequest processorRequest) {
        /* We cannot deploy Processors to a Bridge that is not Available */
        Bridge bridge = bridgesService.getReadyBridge(bridgeId, customerId);

        if (processorDAO.findByBridgeIdAndName(bridgeId, processorRequest.getName()) != null) {
            throw new AlreadyExistingItemException("Processor with name '" + processorRequest.getName() + "' already exists for bridge with id '" + bridgeId + "' for customer '" + customerId + "'");
        }

        Processor newProcessor = new Processor();
        newProcessor.setName(processorRequest.getName());
        newProcessor.setSubmittedAt(ZonedDateTime.now());
        newProcessor.setStatus(ManagedResourceStatus.ACCEPTED);
        newProcessor.setBridge(bridge);
        newProcessor.setShardId(shardService.getAssignedShardId(newProcessor.getId()));

        Set<BaseFilter> requestedFilters = processorRequest.getFilters();

        String requestedTransformationTemplate = processorRequest.getTransformationTemplate();

        boolean isSourceProcessor = processorRequest.getSource() != null;

        Action resolvedAction = isSourceProcessor
                ? resolveAction(processorRequest.getSource(), customerId, bridge.getId(), newProcessor.getId())
                : resolveAction(processorRequest.getAction(), customerId, bridge.getId(), newProcessor.getId());

        ProcessorDefinition definition = isSourceProcessor
                ? new ProcessorDefinition(requestedFilters, requestedTransformationTemplate, processorRequest.getSource(), resolvedAction)
                : new ProcessorDefinition(requestedFilters, requestedTransformationTemplate, processorRequest.getAction(), resolvedAction);

        newProcessor.setDefinition(definitionToJsonNode(definition));

        // Processor, Connector and Work should always be created in the same transaction
        processorDAO.persist(newProcessor);
        if (isSourceProcessor) {
            connectorService.createConnectorEntity(newProcessor, definition.getRequestedSource());
        } else {
            connectorService.createConnectorEntity(newProcessor, definition.getRequestedAction());
        }
        workManager.schedule(newProcessor);

        LOGGER.info("Processor with id '{}' for customer '{}' on bridge '{}' has been marked for creation",
                newProcessor.getId(),
                newProcessor.getBridge().getCustomerId(),
                newProcessor.getBridge().getId());

        return newProcessor;
    }

    private Action resolveAction(Source source, String customerId, String bridgeId, String processorId) {
        Action resolvedAction = gatewayConfigurator.getSourceResolver(source.getType())
                .resolve(source, customerId, bridgeId, processorId);
        return resolveAction(resolvedAction, customerId, bridgeId, processorId);
    }

    private Action resolveAction(Action action, String customerId, String bridgeId, String processorId) {
        Optional<ActionResolver> optActionResolver = gatewayConfigurator.getActionResolver(action.getType());
        if (optActionResolver.isEmpty()) {
            return action;
        }
        Action resolvedAction = optActionResolver.get()
                .resolve(action, customerId, bridgeId, processorId);
        return resolveAction(resolvedAction, customerId, bridgeId, processorId);
    }

    @Override
    @Transactional
    public Processor updateProcessor(String bridgeId, String processorId, String customerId, ProcessorRequest processorRequest) {
        // Attempt to load the Bridge. We cannot update a Processor if the Bridge is not available.
        bridgesService.getReadyBridge(bridgeId, customerId);

        // Extract existing definition
        Processor existingProcessor = getProcessor(bridgeId, processorId, customerId);
        if (!isProcessorActionable(existingProcessor)) {
            throw new ProcessorLifecycleException(String.format("Processor with id '%s' for customer '%s' is not in an actionable state.",
                    processorId,
                    customerId));
        }
        ProcessorDefinition existingDefinition = jsonNodeToDefinition(existingProcessor.getDefinition());
        Action existingAction = existingDefinition.getRequestedAction();
        Action existingResolvedAction = existingDefinition.getResolvedAction();

        // Validate update.
        // Name cannot be updated.
        if (!Objects.equals(existingProcessor.getName(), processorRequest.getName())) {
            throw new BadRequestException("It is not possible to update the Processor's name.");
        }
        // See https://issues.redhat.com/browse/MGDOBR-516 for updating Action support
        if (!Objects.equals(existingAction, processorRequest.getAction())) {
            throw new BadRequestException("It is not possible to update the Processor's Action.");
        }

        // Create new definition copying existing properties
        existingProcessor.setModifiedAt(ZonedDateTime.now());
        existingProcessor.setStatus(ManagedResourceStatus.ACCEPTED);

        Set<BaseFilter> updatedFilters = processorRequest.getFilters();
        String updatedTransformationTemplate = processorRequest.getTransformationTemplate();
        ProcessorDefinition updatedDefinition = new ProcessorDefinition(updatedFilters, updatedTransformationTemplate, existingAction, existingResolvedAction);
        existingProcessor.setDefinition(definitionToJsonNode(updatedDefinition));

        // Processor and Work should always be created in the same transaction
        // Since updates to the Action are unsupported we do not need to update the Connector record.
        workManager.schedule(existingProcessor);

        LOGGER.info("Processor with id '{}' for customer '{}' on bridge '{}' has been marked for update",
                existingProcessor.getId(),
                existingProcessor.getBridge().getCustomerId(),
                existingProcessor.getBridge().getId());

        return existingProcessor;
    }

    @Transactional
    @Override
    public List<Processor> findByShardIdWithReadyDependencies(String shardId) {
        return processorDAO.findByShardIdWithReadyDependencies(shardId);
    }

    @Transactional
    @Override
    public Processor updateProcessorStatus(ProcessorDTO processorDTO) {
        Bridge bridge = bridgesService.getBridge(processorDTO.getBridgeId());
        Processor p = processorDAO.findById(processorDTO.getId());
        if (p == null) {
            throw new ItemNotFoundException(String.format("Processor with id '%s' does not exist for Bridge '%s' for customer '%s'", bridge.getId(), bridge.getCustomerId(),
                    processorDTO.getCustomerId()));
        }
        p.setStatus(processorDTO.getStatus());
        p.setModifiedAt(ZonedDateTime.now());

        if (processorDTO.getStatus().equals(ManagedResourceStatus.DELETED)) {
            processorDAO.deleteById(processorDTO.getId());
        }
        if (processorDTO.getStatus().equals(ManagedResourceStatus.READY) && Objects.isNull(p.getPublishedAt())) {
            p.setPublishedAt(ZonedDateTime.now());
        }

        // Update metrics
        meterRegistry.counter("manager.processor.status.change",
                Collections.singletonList(Tag.of("status", processorDTO.getStatus().toString()))).increment();

        return p;
    }

    @Transactional
    @Override
    public Long getProcessorsCount(String bridgeId, String customerId) {
        return processorDAO.countByBridgeIdAndCustomerId(bridgeId, customerId);
    }

    @Transactional
    @Override
    public ListResult<Processor> getProcessors(String bridgeId, String customerId, QueryInfo queryInfo) {
        Bridge bridge = bridgesService.getReadyBridge(bridgeId, customerId);
        return processorDAO.findByBridgeIdAndCustomerId(bridge.getId(), bridge.getCustomerId(), queryInfo);
    }

    @Override
    @Transactional
    public void deleteProcessor(String bridgeId, String processorId, String customerId) {
        Processor processor = processorDAO.findByIdBridgeIdAndCustomerId(bridgeId, processorId, customerId);
        if (processor == null) {
            throw new ItemNotFoundException(String.format("Processor with id '%s' does not exist on bridge '%s' for customer '%s'", processorId, bridgeId, customerId));
        }
        if (!isProcessorActionable(processor)) {
            throw new ProcessorLifecycleException("Processor could only be deleted if its in READY/FAILED state.");
        }

        // Processor and Connector deletion and related Work creation should always be in the same transaction
        processor.setStatus(ManagedResourceStatus.DEPROVISION);
        connectorService.deleteConnectorEntity(processor);
        workManager.schedule(processor);

        LOGGER.info("Processor with id '{}' for customer '{}' on bridge '{}' has been marked for deletion",
                processor.getId(),
                processor.getBridge().getCustomerId(),
                processor.getBridge().getId());
    }

    private boolean isProcessorActionable(Processor processor) {
        // bridge could only be deleted if its in READY or FAILED state
        return processor.getStatus() == ManagedResourceStatus.READY || processor.getStatus() == ManagedResourceStatus.FAILED;
    }

    @Override
    public ProcessorDTO toDTO(Processor processor) {
        ProcessorDefinition definition = processor.getDefinition() != null ? jsonNodeToDefinition(processor.getDefinition()) : null;

        String topicName = definition.getRequestedSource() != null
                ? resourceNamesProvider.getProcessorTopicName(processor.getId())
                : resourceNamesProvider.getBridgeTopicName(processor.getBridge().getId());

        KafkaConnectionDTO kafkaConnectionDTO = new KafkaConnectionDTO(
                internalKafkaConfigurationProvider.getBootstrapServers(),
                internalKafkaConfigurationProvider.getClientId(),
                internalKafkaConfigurationProvider.getClientSecret(),
                internalKafkaConfigurationProvider.getSecurityProtocol(),
                topicName);
        return new ProcessorDTO(processor.getId(),
                processor.getName(),
                definition,
                processor.getBridge().getId(),
                processor.getBridge().getCustomerId(),
                processor.getStatus(),
                kafkaConnectionDTO);
    }

    @Override
    public ProcessorResponse toResponse(Processor processor) {
        ProcessorResponse processorResponse = new ProcessorResponse();

        processorResponse.setId(processor.getId());
        processorResponse.setName(processor.getName());
        processorResponse.setStatus(processor.getStatus());
        processorResponse.setPublishedAt(processor.getPublishedAt());
        processorResponse.setSubmittedAt(processor.getSubmittedAt());

        if (processor.getDefinition() != null) {
            ProcessorDefinition definition = jsonNodeToDefinition(processor.getDefinition());
            processorResponse.setFilters(definition.getFilters());
            processorResponse.setTransformationTemplate(definition.getTransformationTemplate());
            processorResponse.setAction(definition.getRequestedAction());
            processorResponse.setSource(definition.getRequestedSource());
        }

        if (processor.getBridge() != null) {
            processorResponse.setHref(APIConstants.USER_API_BASE_PATH + processor.getBridge().getId() + "/processors/" + processor.getId());
        }

        return processorResponse;
    }

    JsonNode definitionToJsonNode(ProcessorDefinition definition) {
        return mapper.valueToTree(definition);
    }

    ProcessorDefinition jsonNodeToDefinition(JsonNode jsonNode) {
        try {
            return mapper.treeToValue(jsonNode, ProcessorDefinition.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Can't convert JsonNode to ProcessorDefinition", e);
        }
    }
}
