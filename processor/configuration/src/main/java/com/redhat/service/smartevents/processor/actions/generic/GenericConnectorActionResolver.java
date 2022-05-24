package com.redhat.service.smartevents.processor.actions.generic;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.processor.GatewayConfiguratorService;
import com.redhat.service.smartevents.processor.GatewayResolver;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;

@ApplicationScoped
public abstract class GenericConnectorActionResolver implements GatewayResolver<Action> {

    @Inject
    GatewayConfiguratorService gatewayConfiguratorService;

    @Override
    public Action resolve(Action action, String customerId, String bridgeId, String processorId) {

        Action resolvedAction = new Action();

        ObjectNode newParameters = resolvedAction.getParameters();
        newParameters.setAll(action.getParameters());

        resolvedAction.setType(KafkaTopicAction.TYPE);

        newParameters.put(KafkaTopicAction.TOPIC_PARAM, gatewayConfiguratorService.getConnectorTopicName(processorId));

        return resolvedAction;
    }
}
