package com.redhat.service.rhose.processor.actions;

public interface ActionBean {
    String getType();

    default boolean accept(String actionType) {
        return getType().equals(actionType);
    }
}
