package com.redhat.service.smartevents.processor;

import com.redhat.service.smartevents.infra.models.gateways.Gateway;

public interface GatewayBean<T extends Gateway> {
    String getType();

    default boolean accept(String gatewayType) {
        return getType().equals(gatewayType);
    }
}
