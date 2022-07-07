package com.redhat.service.smartevents.manager;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.api.APIConstants;
import com.redhat.service.smartevents.infra.exceptions.definitions.platform.InternalPlatformException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.AlreadyExistingItemException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.BadRequestException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.BridgeLifecycleException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ProcessorLifecycleException;
import com.redhat.service.smartevents.infra.models.ListResult;
import com.redhat.service.smartevents.infra.models.QueryProcessorResourceInfo;
import com.redhat.service.smartevents.infra.models.dto.KafkaConnectionDTO;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.models.dto.ProcessorManagedResourceStatusUpdateDTO;
import com.redhat.service.smartevents.infra.models.filters.BaseFilter;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.infra.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;
import com.redhat.service.smartevents.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.manager.api.models.responses.ProcessorResponse;
import com.redhat.service.smartevents.manager.connectors.ConnectorsService;
import com.redhat.service.smartevents.manager.dao.ProcessorDAO;
import com.redhat.service.smartevents.manager.metrics.MetricsOperation;
import com.redhat.service.smartevents.manager.metrics.MetricsService;
import com.redhat.service.smartevents.manager.models.Bridge;
import com.redhat.service.smartevents.manager.models.Processor;
import com.redhat.service.smartevents.manager.providers.InternalKafkaConfigurationProvider;
import com.redhat.service.smartevents.manager.providers.ResourceNamesProvider;
import com.redhat.service.smartevents.manager.workers.WorkManager;
import com.redhat.service.smartevents.processor.GatewayConfigurator;

import static com.redhat.service.smartevents.manager.SecretsService.mergeDefinitions;

@ApplicationScoped
public class ProcessorServiceImpl implements ProcessorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessorServiceImpl.class);

    @Inject
    GatewayConfigurator gatewayConfigurator;
    @Inject
    InternalKafkaConfigurationProvider internalKafkaConfigurationProvider;
    @Inject
    ResourceNamesProvider resourceNamesProvider;

    @Inject
    ProcessorDAO processorDAO;

    @Inject
    BridgesService bridgesService;
    @Inject
    ConnectorsService connectorService;
    @Inject
    SecretsService secretsService;
    @Inject
    ShardService shardService;
    @Inject
    WorkManager workManager;

    @Inject
    MetricsService metricsService;

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
    public Optional<Processor> getErrorHandler(String bridgeId, String customerId) {
        ListResult<Processor> hiddenProcessors = processorDAO.findHiddenByBridgeIdAndCustomerId(bridgeId, customerId, new QueryProcessorResourceInfo());
        return hiddenProcessors.getItems()
                .stream()
                .filter(p -> p.getType() == ProcessorType.ERROR_HANDLER)
                .findFirst();
    }

    @Override
    @Transactional
    public Processor createProcessor(String bridgeId, String customerId, String owner, ProcessorRequest processorRequest) {
        // We cannot deploy Processors to a Bridge that is not available. This throws an Exception if the Bridge is not READY.
        Bridge bridge = bridgesService.getReadyBridge(bridgeId, customerId);

        return doCreateProcessor(bridge, customerId, owner, processorRequest.getType(), processorRequest, 0);
    }

    @Override
    @Transactional
    public Processor createErrorHandlerProcessor(String bridgeId, String customerId, String owner, ProcessorRequest processorRequest) {
        Bridge bridge = bridgesService.getBridge(bridgeId, customerId);

        ProcessorType processorType = processorRequest.getType();
        if (processorType != ProcessorType.SINK) {
            throw new InternalPlatformException("Unable to create Error Handler processor. An Action must be defined.");
        }

        return doCreateProcessor(bridge, customerId, owner, ProcessorType.ERROR_HANDLER, processorRequest, bridge.getGeneration());
    }

    private Processor doCreateProcessor(Bridge bridge, String customerId, String owner, ProcessorType processorType, ProcessorRequest processorRequest, long generation) {
        String bridgeId = bridge.getId();
        if (processorDAO.findByBridgeIdAndName(bridgeId, processorRequest.getName()) != null) {
            throw new AlreadyExistingItemException("Processor with name '" + processorRequest.getName() + "' already exists for bridge with id '" + bridgeId + "' for customer '" + customerId + "'");
        }

        Processor newProcessor = new Processor();
        newProcessor.setType(processorType);
        newProcessor.setName(processorRequest.getName());
        newProcessor.setSubmittedAt(ZonedDateTime.now(ZoneOffset.UTC));
        newProcessor.setStatus(ManagedResourceStatus.ACCEPTED);
        newProcessor.setDependencyStatus(ManagedResourceStatus.ACCEPTED);
        newProcessor.setBridge(bridge);
        newProcessor.setShardId(shardService.getAssignedShard(newProcessor.getId()).getId());
        newProcessor.setGeneration(generation);
        newProcessor.setOwner(owner);
        newProcessor.setHasSecrets(false);

        ProcessorDefinition definition = processorRequestToDefinition(processorRequest, processorType, newProcessor.getId(), bridgeId, customerId);
        newProcessor.setDefinition(definition);

        // this updates the processor "definition" and "hasSecrets" fields if required
        secretsService.maskProcessor(newProcessor);

        // Processor, Connector and Work should always be created in the same transaction
        processorDAO.persist(newProcessor);

        connectorService.createConnectorEntity(newProcessor);
        workManager.schedule(newProcessor);
        metricsService.onOperationStart(newProcessor, MetricsOperation.PROVISION);

        LOGGER.info("Processor with id '{}' for customer '{}' on bridge '{}' has been marked for creation",
                newProcessor.getId(),
                newProcessor.getBridge().getCustomerId(),
                newProcessor.getBridge().getId());

        return newProcessor;
    }

    private ProcessorDefinition processorRequestToDefinition(ProcessorRequest processorRequest, ProcessorType processorType, String processorId, String bridgeId, String customerId) {
        Set<BaseFilter> requestedFilters = processorRequest.getFilters();
        String requestedTransformationTemplate = processorRequest.getTransformationTemplate();

        Action resolvedAction = processorType == ProcessorType.SOURCE
                ? resolveSource(processorRequest.getSource(), customerId, bridgeId, processorId)
                : resolveAction(processorRequest.getAction(), customerId, bridgeId, processorId);

        return processorType == ProcessorType.SOURCE
                ? new ProcessorDefinition(requestedFilters, requestedTransformationTemplate, processorRequest.getSource(), resolvedAction)
                : new ProcessorDefinition(requestedFilters, requestedTransformationTemplate, processorRequest.getAction(), resolvedAction);
    }

    private Action resolveAction(Action action, String customerId, String bridgeId, String processorId) {
        return gatewayConfigurator.getActionResolver(action.getType())
                .map(actionResolver -> actionResolver.resolve(action, customerId, bridgeId, processorId))
                // we need to deep copy the action even if it's the same because the mask/unmask of secrets may cause issues otherwise
                .orElse(action.deepCopy());
    }

    private Action resolveSource(Source source, String customerId, String bridgeId, String processorId) {
        return gatewayConfigurator.getSourceResolver(source.getType())
                .resolve(source, customerId, bridgeId, processorId);
    }

    @Override
    @Transactional
    public Processor updateProcessor(String bridgeId, String processorId, String customerId, ProcessorRequest processorRequest) {
        // Attempt to load the Bridge. We cannot update a Processor if the Bridge is not available.
        bridgesService.getReadyBridge(bridgeId, customerId);
        Processor existingProcessor = getProcessor(bridgeId, processorId, customerId);
        long nextGeneration = existingProcessor.getGeneration() + 1;

        return doUpdateProcessor(bridgeId,
                processorId,
                customerId,
                processorRequest.getType(),
                processorRequest,
                nextGeneration);
    }

    @Override
    @Transactional
    public Processor updateErrorHandlerProcessor(String bridgeId, String processorId, String customerId, ProcessorRequest processorRequest) {
        Bridge bridge = bridgesService.getBridge(bridgeId, customerId);
        long nextGeneration = bridge.getGeneration();

        return doUpdateProcessor(bridgeId,
                processorId,
                customerId,
                ProcessorType.ERROR_HANDLER,
                processorRequest,
                nextGeneration);
    }

    // The parameters to this method are part of a larger hack around ErrorHandlers. ProcessorRequest infers its type
    // from whether it has an Action or a Source defined. However, an ErrorHandler also has an Action defined and hence
    // the inference fails. Therefore, the ProcessorType is passed explicitly to this method and not extracted from
    // ProcessorRequest. See the creation related methods for similar m_a_g_i_c.
    private Processor doUpdateProcessor(String bridgeId,
            String processorId,
            String customerId,
            ProcessorType existingProcessorType,
            ProcessorRequest processorRequest,
            long nextGeneration) {
        Processor existingProcessor = getProcessor(bridgeId, processorId, customerId);

        if (!existingProcessor.isActionable()) {
            throw new ProcessorLifecycleException(String.format("Processor with id '%s' for customer '%s' is not in an actionable state.",
                    processorId,
                    customerId));
        }

        ProcessorDefinition existingDefinition = existingProcessor.getDefinition();
        Action existingAction = existingDefinition.getRequestedAction();
        Source existingSource = existingDefinition.getRequestedSource();
        Action existingResolvedAction = existingDefinition.getResolvedAction();

        // Validate update.
        // Name cannot be updated.
        if (!Objects.equals(existingProcessor.getName(), processorRequest.getName())) {
            throw new BadRequestException("It is not possible to update the Processor's name.");
        }
        if (!Objects.equals(existingProcessor.getType(), existingProcessorType)) {
            throw new BadRequestException("It is not possible to update the Processor's Type.");
        }
        if (existingProcessor.getType() == ProcessorType.SINK && !Objects.equals(existingAction.getType(), processorRequest.getAction().getType())) {
            throw new BadRequestException("It is not possible to update the Processor's Action Type.");
        }
        if (existingProcessor.getType() == ProcessorType.SOURCE && !Objects.equals(existingSource.getType(), processorRequest.getSource().getType())) {
            throw new BadRequestException("It is not possible to update the Processor's Source Type.");
        }

        ProcessorDefinition unmaskedExistingDefinition = secretsService.getUnmaskedProcessorDefinition(existingProcessor);
        ProcessorDefinition requestedDefinition = processorRequestToDefinition(processorRequest, existingProcessor.getType(), existingProcessor.getId(), bridgeId, customerId);
        ProcessorDefinition unmaskedRequestedDefinition = mergeDefinitions(unmaskedExistingDefinition, requestedDefinition);

        // No need to update CRD if the definition is unchanged
        // This will need to change to compare _public_ and _secret_ Gateway parameters
        // See https://issues.redhat.com/browse/MGDOBR-59. The individual components of the ProcessorDefinition
        // are separated here to make the required change more explicit.
        if (Objects.equals(unmaskedExistingDefinition.getFilters(), unmaskedRequestedDefinition.getFilters())
                && Objects.equals(unmaskedExistingDefinition.getTransformationTemplate(), unmaskedRequestedDefinition.getTransformationTemplate())
                && Objects.equals(unmaskedExistingDefinition.getRequestedAction(), unmaskedRequestedDefinition.getRequestedAction())
                && Objects.equals(unmaskedExistingDefinition.getRequestedSource(), unmaskedRequestedDefinition.getRequestedSource())
                && Objects.equals(unmaskedExistingDefinition.getResolvedAction(), unmaskedRequestedDefinition.getResolvedAction())) {
            return existingProcessor;
        }

        // Create new definition copying existing properties
        existingProcessor.setModifiedAt(ZonedDateTime.now(ZoneOffset.UTC));
        existingProcessor.setStatus(ManagedResourceStatus.ACCEPTED);
        existingProcessor.setDependencyStatus(ManagedResourceStatus.ACCEPTED);
        existingProcessor.setDefinition(unmaskedRequestedDefinition);
        existingProcessor.setGeneration(existingProcessor.getGeneration() + 1);

        // this updates the processor "definition" and "hasSecrets" fields if required
        secretsService.maskProcessor(existingProcessor);

        // Processor, Connector and Work should always be created in the same transaction
        // Since updates to the Action are unsupported we do not need to update the Connector record.
        connectorService.updateConnectorEntity(existingProcessor);
        workManager.schedule(existingProcessor);
        metricsService.onOperationStart(existingProcessor, MetricsOperation.MODIFY);

        LOGGER.info("Processor with id '{}' for customer '{}' on bridge '{}' has been marked for update",
                existingProcessor.getId(),
                existingProcessor.getBridge().getCustomerId(),
                existingProcessor.getBridge().getId());

        return existingProcessor;
    }

    @Transactional
    @Override
    public List<Processor> findByShardIdWithReadyDependencies(String shardId) {
        return processorDAO.findByShardIdToDeployOrDelete(shardId);
    }

    @Transactional
    @Override
    public Processor updateProcessorStatus(ProcessorManagedResourceStatusUpdateDTO updateDTO) {
        Bridge bridge = bridgesService.getBridge(updateDTO.getBridgeId());
        Processor p = processorDAO.findById(updateDTO.getId());
        if (bridge == null || p == null) {
            throw new ItemNotFoundException(String.format("Processor with id '%s' does not exist for Bridge '%s' for customer '%s'", updateDTO.getId(), updateDTO.getBridgeId(),
                    updateDTO.getCustomerId()));
        }

        if (ManagedResourceStatus.DELETED == updateDTO.getStatus()) {
            p.setStatus(ManagedResourceStatus.DELETED);
            processorDAO.deleteById(updateDTO.getId());
            metricsService.onOperationComplete(p, MetricsOperation.DELETE);
            return p;
        }

        boolean provisioningCallback = p.getStatus() == ManagedResourceStatus.PROVISIONING;
        p.setStatus(updateDTO.getStatus());

        if (ManagedResourceStatus.READY == updateDTO.getStatus()) {
            if (provisioningCallback) {
                if (p.getPublishedAt() == null) {
                    p.setPublishedAt(ZonedDateTime.now(ZoneOffset.UTC));
                    metricsService.onOperationComplete(p, MetricsOperation.PROVISION);
                } else {
                    metricsService.onOperationComplete(p, MetricsOperation.MODIFY);
                }
            }
        }

        return p;
    }

    @Transactional
    @Override
    public Long getProcessorsCount(String bridgeId, String customerId) {
        return processorDAO.countByBridgeIdAndCustomerId(bridgeId, customerId);
    }

    @Transactional
    @Override
    public ListResult<Processor> getProcessors(String bridgeId, String customerId, QueryProcessorResourceInfo queryInfo) {
        Bridge bridge = bridgesService.getBridge(bridgeId, customerId);
        if (!bridge.isActionable()) {
            throw new BridgeLifecycleException(String.format("Bridge with id '%s' for customer '%s' is not in READY/FAILED state.", bridge.getId(), bridge.getCustomerId()));
        }
        return processorDAO.findUserVisibleByBridgeIdAndCustomerId(bridge.getId(), bridge.getCustomerId(), queryInfo);
    }

    @Override
    @Transactional
    public void deleteProcessor(String bridgeId, String processorId, String customerId) {
        Processor processor = processorDAO.findByIdBridgeIdAndCustomerId(bridgeId, processorId, customerId);
        if (processor == null) {
            throw new ItemNotFoundException(String.format("Processor with id '%s' does not exist on bridge '%s' for customer '%s'", processorId, bridgeId, customerId));
        }
        if (!processor.isActionable()) {
            throw new ProcessorLifecycleException("Processor could only be deleted if its in READY/FAILED state.");
        }

        // Processor and Connector deletion and related Work creation should always be in the same transaction
        processor.setStatus(ManagedResourceStatus.DEPROVISION);
        processor.setDependencyStatus(ManagedResourceStatus.DEPROVISION);
        processor.setDeletionRequestedAt(ZonedDateTime.now(ZoneOffset.UTC));

        connectorService.deleteConnectorEntity(processor);
        workManager.schedule(processor);
        metricsService.onOperationStart(processor, MetricsOperation.DELETE);

        LOGGER.info("Processor with id '{}' for customer '{}' on bridge '{}' has been marked for deletion",
                processor.getId(),
                processor.getBridge().getCustomerId(),
                processor.getBridge().getId());
    }

    @Override
    public ProcessorDTO toDTO(Processor processor) {
        ProcessorDTO dto = new ProcessorDTO();
        dto.setType(processor.getType());
        dto.setId(processor.getId());
        dto.setName(processor.getName());
        dto.setDefinition(secretsService.getUnmaskedProcessorDefinition(processor));
        dto.setBridgeId(processor.getBridge().getId());
        dto.setCustomerId(processor.getBridge().getCustomerId());
        dto.setOwner(processor.getOwner());
        dto.setStatus(processor.getStatus());
        dto.setKafkaConnection(getKafkaConnectorDTO(processor));
        return dto;
    }

    private KafkaConnectionDTO getKafkaConnectorDTO(Processor processor) {
        return new KafkaConnectionDTO(
                internalKafkaConfigurationProvider.getBootstrapServers(),
                internalKafkaConfigurationProvider.getClientId(),
                internalKafkaConfigurationProvider.getClientSecret(),
                internalKafkaConfigurationProvider.getSecurityProtocol(),
                internalKafkaConfigurationProvider.getSaslMechanism(),
                getProcessorTopicName(processor),
                resourceNamesProvider.getBridgeErrorTopicName(processor.getBridge().getId()));
    }

    private String getProcessorTopicName(Processor processor) {
        switch (processor.getType()) {
            case ERROR_HANDLER:
                return resourceNamesProvider.getBridgeErrorTopicName(processor.getBridge().getId());
            case SOURCE:
                return resourceNamesProvider.getProcessorTopicName(processor.getId());
            default:
                return resourceNamesProvider.getBridgeTopicName(processor.getBridge().getId());
        }
    }

    @Override
    public ProcessorResponse toResponse(Processor processor) {
        ProcessorResponse processorResponse = new ProcessorResponse();

        processorResponse.setType(processor.getType());
        processorResponse.setId(processor.getId());
        processorResponse.setName(processor.getName());
        processorResponse.setStatus(processor.getStatus());
        processorResponse.setPublishedAt(processor.getPublishedAt());
        processorResponse.setSubmittedAt(processor.getSubmittedAt());
        processorResponse.setModifiedAt(processor.getModifiedAt());
        processorResponse.setOwner(processor.getOwner());

        if (processor.getDefinition() != null) {
            ProcessorDefinition definition = processor.getDefinition();
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
}
