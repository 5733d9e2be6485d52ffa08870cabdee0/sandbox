package com.redhat.service.smartevents.processor.resolvers;

import com.redhat.service.smartevents.infra.models.gateways.Action;

public interface ActionResolver {
    Action resolve(Action action, String customerId, String bridgeId, String processorId);
}
