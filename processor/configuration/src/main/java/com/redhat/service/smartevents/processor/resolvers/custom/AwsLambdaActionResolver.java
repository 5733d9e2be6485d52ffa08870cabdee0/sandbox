package com.redhat.service.smartevents.processor.resolvers.custom;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.TextNode;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.processor.GatewayConfiguratorService;
import com.redhat.service.smartevents.processor.GatewayResolver;
import com.redhat.service.smartevents.processor.actions.aws.AwsLambdaAction;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;

public class AwsLambdaActionResolver implements AwsLambdaAction,
        GatewayResolver<Action> {

    @Inject
    GatewayConfiguratorService gatewayConfiguratorService;

    @Override
    public Action resolve(Action action, String customerId, String bridgeId, String processorId) {

        Action resolvedAction = new Action();
        resolvedAction.setParameters(action.getParameters().deepCopy());
        resolvedAction.setType(KafkaTopicAction.TYPE);

        String connectorTopicName = gatewayConfiguratorService.getConnectorTopicName(processorId);
        resolvedAction.getParameters().set(KafkaTopicAction.TOPIC_PARAM, new TextNode(connectorTopicName));

        return resolvedAction;
    }
}
