package com.redhat.service.bridge.shard.operator.providers;

// TODO: kafka configs to be removed with https://issues.redhat.com/browse/MGDOBR-123
public interface GlobalConfigurationsProvider {

    String getKafkaClient();

    String getKafkaSecret();

    String getKafkaBootstrapServers();

    String getKafkaSecurityProtocol();

    String getSsoUrl();

    String getSsoClientId();
}
