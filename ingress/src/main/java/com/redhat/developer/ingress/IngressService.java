package com.redhat.developer.ingress;

import io.cloudevents.CloudEvent;

public interface IngressService {

    void processEvent(String name, CloudEvent event);

    // TODO: remove after we move to k8s
    String deploy(String name);

    boolean undeploy(String name);
}
