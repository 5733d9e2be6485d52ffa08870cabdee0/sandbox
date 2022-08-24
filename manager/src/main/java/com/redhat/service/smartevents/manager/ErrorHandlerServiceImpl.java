package com.redhat.service.smartevents.manager;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.manager.providers.InternalKafkaConfigurationProvider;
import com.redhat.service.smartevents.manager.providers.ResourceNamesProvider;

@ApplicationScoped
public class ErrorHandlerServiceImpl implements ErrorHandlerService {

    @Inject
    InternalKafkaConfigurationProvider internalKafkaConfigurationProvider;
    @Inject
    ResourceNamesProvider resourceNamesProvider;

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
}
