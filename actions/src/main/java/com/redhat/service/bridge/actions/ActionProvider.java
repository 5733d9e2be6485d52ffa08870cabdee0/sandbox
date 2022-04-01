package com.redhat.service.bridge.actions;

public interface ActionProvider extends ActionAccepter {
    default boolean isConnectorAction() {
        return false;
    }
}
