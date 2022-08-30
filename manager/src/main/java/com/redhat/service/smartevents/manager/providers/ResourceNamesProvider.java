package com.redhat.service.smartevents.manager.providers;

public interface ResourceNamesProvider {

    String getGlobalErrorTopicName();

    String getBridgeTopicName(String bridgeId);

    String getBridgeErrorTopicName(String bridgeId);

    String getProcessorConnectorName(String processorId);

    String getProcessorTopicName(String processorId);

    String getProcessorSecretName(String processorId);

}
