package com.redhat.service.bridge.processor.actions.slack;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.processor.actions.ActionResolver;
import com.redhat.service.bridge.processor.actions.ActionService;
import com.redhat.service.bridge.processor.actions.kafkatopic.KafkaTopicAction;

@ApplicationScoped
public class SlackActionResolver implements ActionResolver {

    @Inject
    ActionService actionService;

    @Override
    public String getType() {
        return SlackAction.TYPE;
    }

    @Override
    public BaseAction resolve(BaseAction action, String customerId, String bridgeId, String processorId) {

        BaseAction resolvedAction = new BaseAction();

        Map<String, String> newParameters = resolvedAction.getParameters();
        newParameters.putAll(action.getParameters());

        resolvedAction.setType(KafkaTopicAction.TYPE);

        newParameters.put(KafkaTopicAction.TOPIC_PARAM, actionService.getConnectorTopicName(processorId));

        return resolvedAction;
    }
}
