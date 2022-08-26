package com.redhat.service.smartevents.processingerrors;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import com.redhat.service.smartevents.infra.models.ListResult;
import com.redhat.service.smartevents.infra.models.QueryResourceInfo;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.manager.BridgesService;
import com.redhat.service.smartevents.manager.providers.InternalKafkaConfigurationProvider;
import com.redhat.service.smartevents.manager.providers.ResourceNamesProvider;
import com.redhat.service.smartevents.processingerrors.api.models.ProcessingErrorResponse;
import com.redhat.service.smartevents.processingerrors.dao.ProcessingErrorDAO;
import com.redhat.service.smartevents.processingerrors.models.ProcessingError;

@ApplicationScoped
public class ProcessingErrorServiceImpl implements ProcessingErrorService {

    @Inject
    InternalKafkaConfigurationProvider internalKafkaConfigurationProvider;

    @Inject
    ResourceNamesProvider resourceNamesProvider;

    @Inject
    ProcessingErrorDAO processingErrorDAO;

    @Inject
    BridgesService bridgesService;

    @Override
    public Action getDefaultErrorHandlerAction() {
        Action action = new Action();
        action.setType("kafka_topic_sink_0.1");
        action.setMapParameters(Map.of(
                "topic", resourceNamesProvider.getGlobalErrorTopicName(),
                "kafka_broker_url", internalKafkaConfigurationProvider.getBootstrapServers(),
                "kafka_client_id", internalKafkaConfigurationProvider.getClientId(),
                "kafka_client_secret", internalKafkaConfigurationProvider.getClientSecret()));
        return action;
    }

    @Transactional
    @Override
    public ListResult<ProcessingError> getProcessingErrors(String bridgeId, String customerId, QueryResourceInfo queryInfo) {
        return processingErrorDAO.findByBridgeIdOrdered(bridgesService.getReadyBridge(bridgeId, customerId).getId(), queryInfo);
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
