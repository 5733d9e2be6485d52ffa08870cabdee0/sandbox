package com.redhat.service.smartevents.processor.resolvers.custom;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.processor.GatewayConfiguratorService;
import com.redhat.service.smartevents.processor.GatewayResolver;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;

@ApplicationScoped
public class KafkaTopicActionResolver implements KafkaTopicAction,
        GatewayResolver<Action> {

    @Inject
    GatewayConfiguratorService gatewayConfiguratorService;

    @Override
    public Action resolve(Action action, String customerId, String bridgeId, String processorId) {
        Action resolvedAction = new Action();
        resolvedAction.setParameters(action.getParameters().deepCopy());
        resolvedAction.setType(KafkaTopicAction.TYPE);

        ObjectNode actionParameters = resolvedAction.getParameters();
        actionParameters.put(SECURITY_PROTOCOL, gatewayConfiguratorService.getSecurityProtocol());

        return resolvedAction;
    }
}
