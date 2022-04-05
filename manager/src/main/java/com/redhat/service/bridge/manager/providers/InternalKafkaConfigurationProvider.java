package com.redhat.service.bridge.manager.providers;

public interface InternalKafkaConfigurationProvider {
    String getClientId();

    String getClientSecret();

    String getBootstrapServers();

    String getSecurityProtocol();
}
