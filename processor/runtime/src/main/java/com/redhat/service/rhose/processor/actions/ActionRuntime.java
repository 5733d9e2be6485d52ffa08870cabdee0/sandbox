package com.redhat.service.rhose.processor.actions;

public interface ActionRuntime {

    ActionInvokerBuilder getInvokerBuilder(String actionType);
}
