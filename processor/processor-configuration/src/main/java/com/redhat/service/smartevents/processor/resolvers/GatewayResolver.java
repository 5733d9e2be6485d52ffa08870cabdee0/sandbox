package com.redhat.service.smartevents.processor.resolvers;

import com.redhat.service.smartevents.infra.v1.api.models.gateways.Action;
import com.redhat.service.smartevents.infra.v1.api.models.gateways.Gateway;

public interface GatewayResolver<T extends Gateway> {
    Action resolve(T gateway, String customerId, String bridgeId, String processorId);
}
