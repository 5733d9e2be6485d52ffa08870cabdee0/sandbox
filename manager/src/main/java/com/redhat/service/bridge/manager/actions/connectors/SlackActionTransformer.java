package com.redhat.service.bridge.manager.actions.connectors;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.bridge.actions.ActionTransformer;
import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.manager.providers.ResourceNamesProvider;

@ApplicationScoped
public class SlackActionTransformer implements ActionTransformer {

    @Inject
    ResourceNamesProvider resourceNamesProvider;

    @Override
    public BaseAction transform(BaseAction action, String bridgeId, String customerId, String processorId) {

        BaseAction resolvedAction = new BaseAction();

        Map<String, String> newParameters = resolvedAction.getParameters();
        newParameters.putAll(action.getParameters());

        resolvedAction.setType(KafkaTopicAction.TYPE);

        newParameters.put(KafkaTopicAction.TOPIC_PARAM, resourceNamesProvider.getProcessorTopicName(processorId));

        return resolvedAction;
    }
}
