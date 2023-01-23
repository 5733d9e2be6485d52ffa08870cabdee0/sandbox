package com.redhat.service.smartevents.manager.v1.providers;

public interface ResourceNamesProviderV1 {
    String getProcessorConnectorName(String processorId);

    String getProcessorTopicName(String processorId);
}
