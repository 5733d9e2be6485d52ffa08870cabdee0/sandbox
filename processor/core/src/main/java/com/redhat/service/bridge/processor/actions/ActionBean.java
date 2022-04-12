package com.redhat.service.bridge.processor.actions;

public interface ActionBean {
    String getType();

    default boolean accept(String actionType) {
        return getType().equals(actionType);
    }
}
