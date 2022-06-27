package com.redhat.service.smartevents.processor;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;

@ApplicationScoped
public class InternalKafkaConnectionPayloadImpl implements InternalKafkaConnectionPayload {

    @Inject
    GatewayConfiguratorService gatewayConfiguratorService;

    @Override
    public void addInternalKafkaConnectionPayload(String bridgeId, String processorId, ObjectNode actionParameters) {
        String connectorTopicName = gatewayConfiguratorService.getConnectorTopicName(processorId);
        actionParameters.set(KafkaTopicAction.TOPIC_PARAM, new TextNode(connectorTopicName));

        actionParameters.set(KafkaTopicAction.BROKER_URL, new TextNode(gatewayConfiguratorService.getBootstrapServers()));
        actionParameters.set(KafkaTopicAction.CLIENT_ID, new TextNode(gatewayConfiguratorService.getClientId()));
        actionParameters.set(KafkaTopicAction.CLIENT_SECRET, new TextNode(gatewayConfiguratorService.getClientSecret()));
        actionParameters.set(KafkaTopicAction.SECURITY_PROTOCOL, new TextNode(gatewayConfiguratorService.getSecurityProtocol()));
    }
}
