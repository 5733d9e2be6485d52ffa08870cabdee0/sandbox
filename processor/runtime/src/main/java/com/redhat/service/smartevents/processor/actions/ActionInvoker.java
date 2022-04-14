package com.redhat.service.smartevents.processor.actions;

public interface ActionInvoker {
    void onEvent(String event);

    default boolean requiresCloudEvent() {
        return true;
    }
}
