package com.redhat.service.bridge.actions;

public interface ActionProvider extends ActionAccepter {

    ActionTransformer getTransformer();

    default boolean isConnectorAction() {
        return false;
    }
}
