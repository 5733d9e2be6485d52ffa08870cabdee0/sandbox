package com.redhat.service.bridge.processor.actions.common;

public interface ActionAccepter {
    String getType();

    default boolean accept(String actionType) {
        return getType().equals(actionType);
    }
}
