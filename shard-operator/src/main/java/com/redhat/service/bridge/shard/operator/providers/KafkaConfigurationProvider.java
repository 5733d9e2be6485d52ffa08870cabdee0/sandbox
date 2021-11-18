package com.redhat.service.bridge.shard.operator.providers;

// TODO: to be removed with https://issues.redhat.com/browse/MGDOBR-123
public interface KafkaConfigurationProvider {

    String getClient();

    String getSecret();

    String getBootstrapServers();

    String getSecurityProtocol();
}
