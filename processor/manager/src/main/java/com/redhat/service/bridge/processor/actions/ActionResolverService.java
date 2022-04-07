package com.redhat.service.bridge.processor.actions;

public interface ActionResolverService {

    String getBridgeEndpoint(String bridgeId, String customerId);

    String getProcessorTopicName(String processorId);
}
