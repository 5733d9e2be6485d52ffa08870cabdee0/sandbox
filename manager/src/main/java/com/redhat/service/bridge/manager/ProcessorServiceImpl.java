package com.redhat.service.bridge.manager;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
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
import com.redhat.service.bridge.actions.ActionProvider;
import com.redhat.service.bridge.actions.ActionProviderFactory;
import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.exceptions.definitions.user.AlreadyExistingItemException;
import com.redhat.service.bridge.infra.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.bridge.infra.models.ListResult;
import com.redhat.service.bridge.infra.models.QueryInfo;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.infra.models.dto.KafkaConnectionDTO;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.infra.models.filters.BaseFilter;
import com.redhat.service.bridge.infra.models.processors.ProcessorDefinition;
import com.redhat.service.bridge.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.bridge.manager.api.models.responses.ProcessorResponse;
import com.redhat.service.bridge.manager.connectors.ConnectorsService;
import com.redhat.service.bridge.manager.connectors.Events;
import com.redhat.service.bridge.manager.dao.ProcessorDAO;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.manager.models.ConnectorEntity;
import com.redhat.service.bridge.manager.models.Processor;
import com.redhat.service.bridge.manager.providers.InternalKafkaConfigurationProvider;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.vertx.mutiny.core.eventbus.EventBus;

import static com.redhat.service.bridge.manager.connectors.Events.CONNECTOR_CREATED_EVENT;

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
    ActionProviderFactory actionProviderFactory;

    @Inject
    ConnectorsService connectorService;

    @Inject
    InternalKafkaConfigurationProvider internalKafkaConfigurationProvider;

    @Inject
    EventBus eventBus;

    @Inject
    ShardService shardService;

    @Transactional
    @Override
    public Processor getProcessor(String processorId, Bridge bridge) {
        Processor processor = processorDAO.findByIdBridgeIdAndCustomerId(processorId, bridge.getId(), bridge.getCustomerId());
        if (processor == null) {
            throw new ItemNotFoundException(String.format("Processor with id '%s' does not exist on Bridge '%s' for customer '%s'", processorId, bridge.getId(), bridge.getCustomerId()));
        }

        return processor;
    }

    @Override
    public Processor createProcessor(Bridge bridge, ProcessorRequest processorRequest) {
        if (findByBridgeIdAndName(bridge.getId(), processorRequest.getName()) != null) {
            throw new AlreadyExistingItemException(
                    "Processor with name '" + processorRequest.getName() + "' already exists for bridge with id '" + bridge.getId() + "' for customer '" + bridge.getCustomerId() + "'");
        }

        Processor newProcessor = new Processor();

        Set<BaseFilter> requestedFilters = processorRequest.getFilters();

        String requestedTransformationTemplate = processorRequest.getTransformationTemplate();
        BaseAction requestedAction = processorRequest.getAction();
        ActionProvider actionProvider = actionProviderFactory.getActionProvider(requestedAction.getType());

        BaseAction resolvedAction = actionProviderFactory.resolve(requestedAction,
                bridge.getId(),
                bridge.getCustomerId(),
                newProcessor.getId());

        newProcessor.setName(processorRequest.getName());
        newProcessor.setSubmittedAt(ZonedDateTime.now());
        newProcessor.setStatus(BridgeStatus.ACCEPTED);
        newProcessor.setBridge(bridge);
        newProcessor.setShardId(shardService.getAssignedShardId(newProcessor.getId()));

        ProcessorDefinition definition = new ProcessorDefinition(requestedFilters, requestedTransformationTemplate, requestedAction, resolvedAction);
        newProcessor.setDefinition(definitionToJsonNode(definition));

        Optional<ConnectorEntity> persist = createProcessorConnectorEntity(newProcessor, actionProvider, resolvedAction);

        persist.ifPresent(c -> eventBus.requestAndForget(CONNECTOR_CREATED_EVENT, c));

        return newProcessor;
    }

    @Transactional
    private Processor findByBridgeIdAndName(String bridgeId, String name) {
        return processorDAO.findByBridgeIdAndName(bridgeId, name);
    }

    @Transactional
    public Optional<ConnectorEntity> createProcessorConnectorEntity(Processor newProcessor, ActionProvider actionProvider, BaseAction resolvedAction) {
        processorDAO.persist(newProcessor);
        return connectorService.createConnectorEntity(resolvedAction, newProcessor, actionProvider);
    }

    @Transactional
    @Override
    public List<Processor> getProcessorByStatusesAndShardIdWithReadyDependencies(List<BridgeStatus> statuses, String shardId) {
        return processorDAO.findByStatusesAndShardIdWithReadyDependencies(statuses, shardId);
    }

    @Transactional
    @Override
    public Processor updateProcessorStatus(ProcessorDTO processorDTO) {
        Processor p = processorDAO.findById(processorDTO.getId());
        if (p == null) {
            throw new ItemNotFoundException(String.format("Processor with id '%s' does not exist for Bridge '%s' for customer '%s'", processorDTO.getId(), processorDTO.getBridgeId(),
                    processorDTO.getCustomerId()));
        }
        p.setStatus(processorDTO.getStatus());

        if (processorDTO.getStatus().equals(BridgeStatus.DELETED)) {
            processorDAO.deleteById(processorDTO.getId());
        }

        // Update metrics
        meterRegistry.counter("manager.processor.status.change",
                Collections.singletonList(Tag.of("status", processorDTO.getStatus().toString()))).increment();

        return p;
    }

    @Transactional
    @Override
    public Long getProcessorsCount(Bridge bridge) {
        return processorDAO.countByBridgeIdAndCustomerId(bridge.getId(), bridge.getCustomerId());
    }

    @Transactional
    @Override
    public ListResult<Processor> getProcessors(Bridge bridge, QueryInfo queryInfo) {
        return processorDAO.findByBridgeIdAndCustomerId(bridge.getId(), bridge.getCustomerId(), queryInfo);
    }

    @Override
    public void deleteProcessor(Bridge bridge, String processorId) {
        List<ConnectorEntity> connectorEntitiesToBeDeleted = commitDeletionRequest(bridge, processorId);
        connectorEntitiesToBeDeleted.forEach(c -> {
            LOGGER.info("Firing deletion event for entity: " + c);
            eventBus.requestAndForget(Events.CONNECTOR_DELETED_EVENT, c);
        });
    }

    @Transactional
    public List<ConnectorEntity> commitDeletionRequest(Bridge bridge, String processorId) {
        Processor processor = processorDAO.findByIdBridgeIdAndCustomerId(processorId, bridge.getId(), bridge.getCustomerId());
        if (processor == null) {
            throw new ItemNotFoundException(String.format("Processor with id '%s' does not exist on bridge '%s' for customer '%s'", processorId, bridge.getId(), bridge.getCustomerId()));
        }
        processor.setStatus(BridgeStatus.DEPROVISION);

        LOGGER.info("Processor with id '{}' for customer '{}' on bridge '{}' has been marked for deletion", processor.getId(), processor.getBridge().getCustomerId(),
                processor.getBridge().getId());

        return connectorService.deleteConnectorIfNeeded(processor);
    }

    @Override
    public ProcessorDTO toDTO(Processor processor) {
        ProcessorDefinition definition = processor.getDefinition() != null ? jsonNodeToDefinition(processor.getDefinition()) : null;
        KafkaConnectionDTO kafkaConnectionDTO = new KafkaConnectionDTO(
                internalKafkaConfigurationProvider.getBootstrapServers(),
                internalKafkaConfigurationProvider.getClientId(),
                internalKafkaConfigurationProvider.getClientSecret(),
                internalKafkaConfigurationProvider.getSecurityProtocol(),
                bridgesService.getBridgeTopicName(processor.getBridge()));
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
