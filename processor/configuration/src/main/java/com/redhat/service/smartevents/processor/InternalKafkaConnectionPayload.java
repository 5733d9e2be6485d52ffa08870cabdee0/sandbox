package com.redhat.service.smartevents.processor;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface InternalKafkaConnectionPayload {

    void addInternalKafkaConnectionPayload(String bridgeId, String processorId, ObjectNode actionParameters);
}
