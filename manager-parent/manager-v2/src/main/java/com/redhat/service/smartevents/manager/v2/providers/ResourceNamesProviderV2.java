package com.redhat.service.smartevents.manager.v2.providers;

public interface ResourceNamesProviderV2 {
    String getSourceConnectorName(String bridgeId);

    String getSourceConnectorTopicName(String bridgeId);
}
