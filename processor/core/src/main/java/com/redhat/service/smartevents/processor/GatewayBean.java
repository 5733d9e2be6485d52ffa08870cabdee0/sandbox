package com.redhat.service.smartevents.processor;

public interface GatewayBean {
    GatewayFamily getFamily();

    String getType();

    default boolean accept(GatewayFamily family, String type) {
        return getFamily() == family && getType().equals(type);
    }
}
