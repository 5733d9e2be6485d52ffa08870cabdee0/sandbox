package com.redhat.service.smartevents.processor;

import com.redhat.service.smartevents.infra.models.gateways.Gateway;

public interface GatewayResolver<T extends Gateway> extends GatewayBean {

    ResolvedGateway<T> resolve(T gateway, String customerId, String bridgeId, String processorId);
}
