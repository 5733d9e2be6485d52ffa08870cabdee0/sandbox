package com.redhat.service.smartevents.manager.providers;

public interface ResourceNamesProvider {

    String getBridgeTopicName(String bridgeId);

    String getBridgeErrorTopicName(String bridgeId);

    String getProcessorConnectorName(String processorId, String actionName);

    String getProcessorTopicName(String processorId, String actionName);

}
