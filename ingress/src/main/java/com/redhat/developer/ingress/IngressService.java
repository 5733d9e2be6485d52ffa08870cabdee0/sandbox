package com.redhat.developer.ingress;

import io.cloudevents.CloudEvent;

public interface IngressService {

    void processEvent(String id, CloudEvent event);

    // TODO: remove after we move to k8s
    String deploy(String id);

    boolean undeploy(String id);
}
