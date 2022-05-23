package com.redhat.service.smartevents.manager.providers;

public interface ResourceNamesProvider {

    String getBridgeTopicName(String bridgeId);

    String getProcessorConnectorName(String processorId);

    String getProcessorTopicName(String processorId);

    String getErrorHandlerTopicName(String bridgeId);

}
