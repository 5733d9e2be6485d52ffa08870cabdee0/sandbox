package com.redhat.service.smartevents.processor;

public interface GatewayBean {
    String getType();

    default boolean accept(String gatewayType) {
        return getType().equals(gatewayType);
    }
}
