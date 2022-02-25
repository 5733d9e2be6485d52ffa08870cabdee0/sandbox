package com.redhat.service.bridge.manager.providers;

import com.redhat.service.bridge.manager.models.Bridge;

public interface InternalKafkaConfigurationProvider {

    String getClientId();

    String getClientSecret();

    String getBootstrapServers();

    String getSecurityProtocol();

    String getTopicPrefix();

    String getTopicName(Bridge bridge);
}
