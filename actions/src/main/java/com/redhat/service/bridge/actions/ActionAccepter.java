package com.redhat.service.bridge.actions;

public interface ActionAccepter {
    String getType();

    default boolean accept(String actionType) {
        return getType().equals(actionType);
    }
}
