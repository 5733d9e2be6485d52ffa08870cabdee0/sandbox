package com.redhat.service.bridge.manager.actions.connectors;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.bridge.actions.ActionTransformer;
import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.bridge.infra.models.actions.BaseAction;

@ApplicationScoped
public class ConnectorsActionTransformer implements ActionTransformer {

    @Override
    public BaseAction transform(BaseAction action, String bridgeId, String customerId, String processorId) {

        Map<String, String> parameters = new HashMap<>();

        BaseAction resolvedAction = new BaseAction();
        resolvedAction.setType(KafkaTopicAction.TYPE);
        resolvedAction.setName(action.getName());
        resolvedAction.setParameters(parameters);

        parameters.put(KafkaTopicAction.TOPIC_PARAM, generateKafkaTopicName(processorId));

        return resolvedAction;
    }

    // once we use a single topic for every connector there will be no need of having a different
    // one per connector https://issues.redhat.com/browse/MGDSTRM-5977
    private String generateKafkaTopicName(String processorId) {
        String kafkaTopic = UUID.randomUUID().toString();

        return String.format("%s-%s", processorId, kafkaTopic);
    }
}
