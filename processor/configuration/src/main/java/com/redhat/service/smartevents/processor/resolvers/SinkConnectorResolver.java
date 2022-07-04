package com.redhat.service.smartevents.processor.resolvers;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.TextNode;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.processor.GatewayConfiguratorService;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;

@ApplicationScoped
public class SinkConnectorResolver implements GatewayResolver<Action> {

    @Inject
    GatewayConfiguratorService gatewayConfiguratorService;

    @Override
    public Action resolve(Action action, String customerId, String bridgeId, String processorId) {

        Action resolvedAction = new Action();

        resolvedAction.setParameters(action.getParameters().deepCopy());
        resolvedAction.setType(KafkaTopicAction.TYPE);
        resolvedAction.setName(action.getName());

        String connectorTopicName = gatewayConfiguratorService.getConnectorTopicName(processorId, action.getName());

        resolvedAction.getParameters().set(KafkaTopicAction.TOPIC_PARAM, new TextNode(connectorTopicName));
        resolvedAction.getParameters().set(KafkaTopicAction.BROKER_URL, new TextNode(gatewayConfiguratorService.getBootstrapServers()));
        resolvedAction.getParameters().set(KafkaTopicAction.CLIENT_ID, new TextNode(gatewayConfiguratorService.getClientId()));
        resolvedAction.getParameters().set(KafkaTopicAction.CLIENT_SECRET, new TextNode(gatewayConfiguratorService.getClientSecret()));
        resolvedAction.getParameters().set(KafkaTopicAction.SECURITY_PROTOCOL, new TextNode(gatewayConfiguratorService.getSecurityProtocol()));

        return resolvedAction;
    }
}
