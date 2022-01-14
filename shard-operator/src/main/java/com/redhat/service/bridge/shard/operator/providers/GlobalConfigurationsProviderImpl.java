package com.redhat.service.bridge.shard.operator.providers;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class GlobalConfigurationsProviderImpl implements GlobalConfigurationsProvider {

    @ConfigProperty(name = "event-bridge.default.kafka.bootstrap.servers")
    String kafkaBootstrapServers;

    @ConfigProperty(name = "event-bridge.default.kafka.client.id")
    String kafkaClientId;

    @ConfigProperty(name = "event-bridge.default.kafka.client.secret")
    String kafkaClientSecret;

    @ConfigProperty(name = "event-bridge.default.kafka.security.protocol")
    String kafkaSecurityProtocol;

    @ConfigProperty(name = "event-bridge.sso.auth-server-url")
    String ssoUrl;

    @ConfigProperty(name = "event-bridge.sso.client-id")
    String ssoClientId;

    @Override
    public String getKafkaClient() {
        return kafkaClientId;
    }

    @Override
    public String getKafkaSecret() {
        return kafkaClientSecret;
    }

    @Override
    public String getKafkaBootstrapServers() {
        return kafkaBootstrapServers;
    }

    @Override
    public String getKafkaSecurityProtocol() {
        return kafkaSecurityProtocol;
    }

    @Override
    public String getSsoUrl() {
        return ssoUrl;
    }

    @Override
    public String getSsoClientId() {
        return ssoClientId;
    }
}
