package com.redhat.service.bridge.manager.providers;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.redhat.service.bridge.manager.RhoasService;

@ApplicationScoped
public class InternalKafkaConfigurationProviderImpl implements InternalKafkaConfigurationProvider {

    @ConfigProperty(name = "event-bridge.kafka.bootstrap.servers")
    String kafkaBootstrapServers;

    @ConfigProperty(name = "event-bridge.kafka.client.id")
    String kafkaClientId;

    @ConfigProperty(name = "event-bridge.kafka.client.secret")
    String kafkaClientSecret;

    @ConfigProperty(name = "event-bridge.kafka.security.protocol")
    String kafkaSecurityProtocol;

    @Inject
    RhoasService rhoasService;

    @Override
    public String getClientId() {
        return kafkaClientId;
    }

    @Override
    public String getClientSecret() {
        return kafkaClientSecret;
    }

    @Override
    public String getBootstrapServers() {
        return kafkaBootstrapServers;
    }

    @Override
    public String getSecurityProtocol() {
        return kafkaSecurityProtocol;
    }
}
