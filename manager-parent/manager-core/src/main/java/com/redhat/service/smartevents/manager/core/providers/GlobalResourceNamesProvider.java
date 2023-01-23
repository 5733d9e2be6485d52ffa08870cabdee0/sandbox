package com.redhat.service.smartevents.manager.core.providers;

public interface GlobalResourceNamesProvider {

    String getGlobalErrorTopicName();

    String getBridgeTopicName(String bridgeId);

    String getBridgeErrorTopicName(String bridgeId);

}
