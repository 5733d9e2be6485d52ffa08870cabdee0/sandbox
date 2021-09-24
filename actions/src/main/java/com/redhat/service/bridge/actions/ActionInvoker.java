package com.redhat.service.bridge.actions;

import io.cloudevents.CloudEvent;

public interface ActionInvoker {
    void onEvent(CloudEvent cloudEvent);
}
