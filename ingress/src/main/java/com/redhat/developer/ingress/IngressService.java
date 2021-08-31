package com.redhat.developer.ingress;

import com.fasterxml.jackson.databind.JsonNode;

public interface IngressService {
    boolean processEvent(String name, JsonNode event);

    // TODO: remove after we move to k8s
    String deploy(String name);
    boolean undeploy(String name);
}
