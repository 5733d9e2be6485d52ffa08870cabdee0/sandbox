package com.redhat.service.smartevents.processor.actions.slack;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.processor.GatewayConfiguratorService;
import com.redhat.service.smartevents.processor.actions.ActionResolver;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;

@ApplicationScoped
public class SlackActionResolver implements ActionResolver {

    @Inject
    GatewayConfiguratorService gatewayConfiguratorService;

    @Override
    public String getType() {
        return SlackAction.TYPE;
    }

    @Override
    public Action resolve(Action action, String customerId, String bridgeId, String processorId) {

        Action resolvedAction = new Action();

        Map<String, String> newParameters = resolvedAction.getParameters();
        newParameters.putAll(action.getParameters());

        resolvedAction.setType(KafkaTopicAction.TYPE);

        newParameters.put(KafkaTopicAction.TOPIC_PARAM, gatewayConfiguratorService.getConnectorTopicName(processorId));

        return resolvedAction;
    }
}
