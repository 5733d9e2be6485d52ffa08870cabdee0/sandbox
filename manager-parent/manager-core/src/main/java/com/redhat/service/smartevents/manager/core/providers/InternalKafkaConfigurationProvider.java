package com.redhat.service.smartevents.manager.core.providers;

public interface InternalKafkaConfigurationProvider {
    String getClientId();

    String getClientSecret();

    String getBootstrapServers();

    String getSecurityProtocol();

    String getSaslMechanism();
}
