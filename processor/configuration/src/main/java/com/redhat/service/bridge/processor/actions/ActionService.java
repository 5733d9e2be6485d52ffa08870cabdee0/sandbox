package com.redhat.service.bridge.processor.actions;

public interface ActionService {

    String getBridgeEndpoint(String bridgeId, String customerId);

    String getConnectorTopicName(String processorId);
}
