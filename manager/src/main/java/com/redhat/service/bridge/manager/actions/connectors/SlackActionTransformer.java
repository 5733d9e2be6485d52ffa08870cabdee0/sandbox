package com.redhat.service.bridge.manager.actions.connectors;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.redhat.service.bridge.actions.ActionTransformer;
import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.manager.RhoasService;
import com.redhat.service.bridge.manager.config.ConfigUtils;

@ApplicationScoped
public class SlackActionTransformer implements ActionTransformer {

    public static final String TOPIC_PREFIX = ConfigUtils.topicPrefix();

    @ConfigProperty(name = "managed-connectors.topic-name")
    String topicName;

    @Inject
    RhoasService rhoasService;

    @Override
    public BaseAction transform(BaseAction action, String bridgeId, String customerId, String processorId) {

        BaseAction resolvedAction = new BaseAction();

        Map<String, String> newParameters = resolvedAction.getParameters();
        newParameters.putAll(action.getParameters());

        resolvedAction.setType(KafkaTopicAction.TYPE);
        resolvedAction.setName(action.getName());

        newParameters.put(KafkaTopicAction.TOPIC_PARAM, generateKafkaTopicName(processorId));

        return resolvedAction;
    }

    // Currently, it's just a fixed topic for testing purposes.
    // When https://issues.redhat.com/browse/MGDOBR-168 is ready, we can generate one for connector
    // once we use a single topic for every connector there will be no need of having a different
    // one per connector https://issues.redhat.com/browse/MGDSTRM-5977
    private String generateKafkaTopicName(String processorId) {
        return TOPIC_PREFIX + processorId;
    }
}
