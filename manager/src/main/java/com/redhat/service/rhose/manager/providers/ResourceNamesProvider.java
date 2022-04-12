package com.redhat.service.rhose.manager.providers;

public interface ResourceNamesProvider {

    String getBridgeTopicName(String bridgeId);

    String getProcessorConnectorName(String processorId);

    String getProcessorTopicName(String processorId);

}
