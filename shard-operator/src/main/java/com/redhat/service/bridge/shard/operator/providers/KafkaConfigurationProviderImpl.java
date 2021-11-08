package com.redhat.service.bridge.shard.operator.providers;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class KafkaConfigurationProviderImpl implements KafkaConfigurationProvider {

    @ConfigProperty(name = "event-bridge.default.kafka.bootstrap.servers")
    String kafkaBootstapServers;

    @ConfigProperty(name = "event-bridge.default.kafka.client.id")
    String kafkaClientId;

    @ConfigProperty(name = "event-bridge.default.kafka.client.secret")
    String kafkaClientSecret;

    @Override
    public String getClient() {
        return kafkaClientId;
    }

    @Override
    public String getSecret() {
        return kafkaClientSecret;
    }

    @Override
    public String getBootstrapServers() {
        return kafkaBootstapServers;
    }
}
