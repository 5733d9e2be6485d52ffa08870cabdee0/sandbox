package com.redhat.service.smartevents.processor.actions;

import io.cloudevents.CloudEvent;

public interface ActionInvoker {
    void onEvent(CloudEvent originalEvent, String transformedEvent);
}
