package com.redhat.service.bridge.executor.actions;

import io.cloudevents.CloudEvent;

public interface ActionInvoker {
    void onEvent(CloudEvent cloudEvent);
}
