package com.redhat.service.bridge.manager.resolvers;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.manager.providers.ResourceNamesProvider;
import com.redhat.service.bridge.processor.actions.kafkatopic.KafkaTopicActionBean;
import com.redhat.service.bridge.processor.actions.slack.SlackActionBean;

@ApplicationScoped
public class SlackActionResolver implements ActionResolver {

    @Inject
    ResourceNamesProvider resourceNamesProvider;

    @Override
    public String getType() {
        return SlackActionBean.TYPE;
    }

    @Override
    public BaseAction resolve(BaseAction action, String customerId, String bridgeId, String processorId) {

        BaseAction resolvedAction = new BaseAction();

        Map<String, String> newParameters = resolvedAction.getParameters();
        newParameters.putAll(action.getParameters());

        resolvedAction.setType(KafkaTopicActionBean.TYPE);

        newParameters.put(KafkaTopicActionBean.TOPIC_PARAM, resourceNamesProvider.getProcessorTopicName(processorId));

        return resolvedAction;
    }
}
