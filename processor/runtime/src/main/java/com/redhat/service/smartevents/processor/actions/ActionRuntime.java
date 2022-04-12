package com.redhat.service.smartevents.processor.actions;

public interface ActionRuntime {

    ActionInvokerBuilder getInvokerBuilder(String actionType);
}
