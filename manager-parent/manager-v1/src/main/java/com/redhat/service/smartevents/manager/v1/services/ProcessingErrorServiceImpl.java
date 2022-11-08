package com.redhat.service.smartevents.manager.v1.services;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.BadRequestException;
import com.redhat.service.smartevents.infra.core.models.ListResult;
import com.redhat.service.smartevents.infra.core.models.queries.QueryResourceInfo;
import com.redhat.service.smartevents.infra.v1.api.V1APIConstants;
import com.redhat.service.smartevents.infra.v1.api.models.gateways.Action;
import com.redhat.service.smartevents.manager.core.api.models.responses.ProcessingErrorResponse;
import com.redhat.service.smartevents.manager.core.persistence.dao.ProcessingErrorDAO;
import com.redhat.service.smartevents.manager.core.persistence.models.ProcessingError;
import com.redhat.service.smartevents.manager.core.providers.InternalKafkaConfigurationProvider;
import com.redhat.service.smartevents.manager.core.providers.ResourceNamesProvider;
import com.redhat.service.smartevents.manager.v1.persistence.models.Bridge;

@ApplicationScoped
public class ProcessingErrorServiceImpl implements ProcessingErrorService {

    public static final String ENDPOINT_RESOLVED_ERROR_HANDLER_TYPE = "kafka_topic_sink_0.1";
    public static final String ENDPOINT_RESOLVED_ERROR_HANDLER_PARAM_TOPIC = "topic";
    public static final String ENDPOINT_RESOLVED_ERROR_HANDLER_PARAM_KAFKA_BROKER_URL = "kafka_broker_url";
    public static final String ENDPOINT_RESOLVED_ERROR_HANDLER_PARAM_KAFKA_CLIENT_ID = "kafka_client_id";
    public static final String ENDPOINT_RESOLVED_ERROR_HANDLER_PARAM_KAFKA_CLIENT_SECRET = "kafka_client_secret";

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

    @Transactional
    @Override
    public ListResult<ProcessingError> getProcessingErrors(String bridgeId, String customerId, QueryResourceInfo queryInfo) {
        Bridge bridge = bridgesService.getReadyBridge(bridgeId, customerId);
        if (!ProcessingErrorService.isEndpointErrorHandlerAction(bridge.getDefinition().getErrorHandler())) {
            throw new BadRequestException(String.format("Bridge %s is not configured to store processing errors", bridgeId));
        }
        return processingErrorDAO.findByBridgeIdOrdered(bridge.getId(), queryInfo);
    }

    @Override
    public Action resolveAndUpdateErrorHandler(String bridgeId, Action errorHandler) {
        if (!ProcessingErrorService.isEndpointErrorHandlerAction(errorHandler)) {
            return errorHandler;
        }
        errorHandler.setMapParameters(Map.of("endpoint", getErrorEndpoint(bridgeId)));
        return getEndpointErrorHandlerResolvedAction();
    }

    @Override
    public ProcessingErrorResponse toResponse(ProcessingError processingError) {
        ProcessingErrorResponse response = new ProcessingErrorResponse();
        response.setRecordedAt(processingError.getRecordedAt());
        response.setHeaders(processingError.getHeaders());
        response.setPayload(processingError.getPayload());
        return response;
    }

    private Action getEndpointErrorHandlerResolvedAction() {
        Action action = new Action();
        action.setType(ENDPOINT_RESOLVED_ERROR_HANDLER_TYPE);
        action.setMapParameters(Map.of(
                ENDPOINT_RESOLVED_ERROR_HANDLER_PARAM_TOPIC, resourceNamesProvider.getGlobalErrorTopicName(),
                ENDPOINT_RESOLVED_ERROR_HANDLER_PARAM_KAFKA_BROKER_URL, internalKafkaConfigurationProvider.getBootstrapServers(),
                ENDPOINT_RESOLVED_ERROR_HANDLER_PARAM_KAFKA_CLIENT_ID, internalKafkaConfigurationProvider.getClientId(),
                ENDPOINT_RESOLVED_ERROR_HANDLER_PARAM_KAFKA_CLIENT_SECRET, internalKafkaConfigurationProvider.getClientSecret()));
        return action;
    }

    private String getErrorEndpoint(String bridgeId) {
        return eventBridgeManagerUrl + V1APIConstants.V1_USER_API_BASE_PATH + bridgeId + "/errors";
    }
}
