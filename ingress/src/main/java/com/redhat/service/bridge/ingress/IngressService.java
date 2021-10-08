package com.redhat.service.bridge.ingress;

import com.fasterxml.jackson.databind.JsonNode;

public interface IngressService {

    void processEvent(String id, JsonNode event);

    // TODO: remove after we move to k8s
    String deploy(String id);

    boolean undeploy(String id);
}
