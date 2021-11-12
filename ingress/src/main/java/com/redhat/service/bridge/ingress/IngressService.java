package com.redhat.service.bridge.ingress;

import io.cloudevents.CloudEvent;

public interface IngressService {
    void processEvent(CloudEvent event);
}
