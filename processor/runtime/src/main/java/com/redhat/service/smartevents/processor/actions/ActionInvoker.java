package com.redhat.service.smartevents.processor.actions;

public interface ActionInvoker {
    void onEvent(String bridgeId, String processorId, String originalEventId, String transformedEvent);
}
