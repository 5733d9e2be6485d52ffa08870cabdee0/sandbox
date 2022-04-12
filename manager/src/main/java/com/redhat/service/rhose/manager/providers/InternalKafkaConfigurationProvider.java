package com.redhat.service.rhose.manager.providers;

public interface InternalKafkaConfigurationProvider {
    String getClientId();

    String getClientSecret();

    String getBootstrapServers();

    String getSecurityProtocol();
}
