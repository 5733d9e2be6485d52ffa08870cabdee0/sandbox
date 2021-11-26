package com.redhat.service.bridge.actions;

public interface ActionProvider {

    default boolean accept(String actionType) {
        return getType().equals(actionType);
    }

    String getType();

    ActionParameterValidator getParameterValidator();

    ActionTransformer getTransformer();

}
