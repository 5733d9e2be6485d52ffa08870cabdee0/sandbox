package com.redhat.service.smartevents.processingerrors;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.redhat.service.smartevents.infra.api.APIConstants;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.BadRequestException;
import com.redhat.service.smartevents.infra.models.ListResult;
import com.redhat.service.smartevents.infra.models.QueryResourceInfo;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.manager.BridgesService;
import com.redhat.service.smartevents.manager.models.Bridge;
import com.redhat.service.smartevents.manager.providers.InternalKafkaConfigurationProvider;
import com.redhat.service.smartevents.manager.providers.ResourceNamesProvider;
import com.redhat.service.smartevents.processingerrors.api.models.ProcessingErrorResponse;
import com.redhat.service.smartevents.processingerrors.dao.ProcessingErrorDAO;
import com.redhat.service.smartevents.processingerrors.models.ProcessingError;

@ApplicationScoped
public class ProcessingErrorServiceImpl implements ProcessingErrorService {

    @ConfigProperty(name = "event-bridge.manager.url")
    String eventBridgeManagerUrl;

    @Inject
    InternalKafkaConfigurationProvider internalKafkaConfigurationProvider;

    @Inject
    ResourceNamesProvider resourceNamesProvider;

    @Inject
    ProcessingErrorDAO processingErrorDAO;

    @Inject
    BridgesService bridgesService;

    @Override
    public Action getEndpointErrorHandlerResolvedAction() {
        Action action = new Action();
        action.setType("kafka_topic_sink_0.1");
        action.setMapParameters(Map.of(
                "topic", resourceNamesProvider.getGlobalErrorTopicName(),
                "kafka_broker_url", internalKafkaConfigurationProvider.getBootstrapServers(),
                "kafka_client_id", internalKafkaConfigurationProvider.getClientId(),
                "kafka_client_secret", internalKafkaConfigurationProvider.getClientSecret()));
        return action;
    }

    @Override
    public String getErrorEndpoint(String bridgeId) {
        return eventBridgeManagerUrl + APIConstants.USER_API_BASE_PATH + bridgeId + "/errors";
    }

    @Transactional
    @Override
    public ListResult<ProcessingError> getProcessingErrors(String bridgeId, String customerId, QueryResourceInfo queryInfo) {
        Bridge bridge = bridgesService.getReadyBridge(bridgeId, customerId);
        if (!bridge.getDefinition().hasEndpointErrorHandler()) {
            throw new BadRequestException(String.format("Bridge %s is not configured to store processing errors", bridgeId));
        }
        return processingErrorDAO.findByBridgeIdOrdered(bridge.getId(), queryInfo);
    }

    @Override
    public ProcessingErrorResponse toResponse(ProcessingError processingError) {
        ProcessingErrorResponse response = new ProcessingErrorResponse();
        response.setRecordedAt(processingError.getRecordedAt());
        response.setHeaders(processingError.getHeaders());
        response.setPayload(processingError.getPayload());
        return response;
    }
}
