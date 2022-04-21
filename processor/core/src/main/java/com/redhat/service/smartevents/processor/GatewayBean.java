package com.redhat.service.smartevents.processor;

import java.util.Objects;

public interface GatewayBean {
    String getType();

    default boolean accept(String gatewayType) {
        return Objects.equals(getType(), gatewayType);
    }
}
