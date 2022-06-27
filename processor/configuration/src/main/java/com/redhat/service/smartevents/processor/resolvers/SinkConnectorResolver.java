package com.redhat.service.smartevents.processor.resolvers;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.processor.InternalKafkaConnectionPayload;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;

@ApplicationScoped
public class SinkConnectorResolver implements GatewayResolver<Action> {

    @Inject
    InternalKafkaConnectionPayload internalKafkaConnectionPayload;

    @Override
    public Action resolve(Action action, String customerId, String bridgeId, String processorId) {

        Action resolvedAction = new Action();

        resolvedAction.setParameters(action.getParameters().deepCopy());
        resolvedAction.setType(KafkaTopicAction.TYPE);

        internalKafkaConnectionPayload.addInternalKafkaConnectionPayload(bridgeId, processorId, resolvedAction.getParameters());

        return resolvedAction;
    }
}
