package com.redhat.service.smartevents.processor.resolvers;

import com.redhat.service.smartevents.infra.core.models.gateways.Action;
import com.redhat.service.smartevents.infra.core.models.gateways.Gateway;

public interface GatewayResolver<T extends Gateway> {
    Action resolve(T gateway, String customerId, String bridgeId, String processorId);
}
