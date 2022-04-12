package com.redhat.service.smartevents.manager.providers;

public interface InternalKafkaConfigurationProvider {
    String getClientId();

    String getClientSecret();

    String getBootstrapServers();

    String getSecurityProtocol();
}
