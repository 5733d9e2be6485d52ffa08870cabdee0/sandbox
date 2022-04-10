package com.redhat.service.bridge.processor.actions;

public interface ActionRuntime {

    ActionInvokerBuilder getInvokerBuilder(String actionType);
}
