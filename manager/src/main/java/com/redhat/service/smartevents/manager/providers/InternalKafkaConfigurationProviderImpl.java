package com.redhat.service.smartevents.manager.providers;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class InternalKafkaConfigurationProviderImpl implements InternalKafkaConfigurationProvider {

    public static final String KAFKA_BOOTSTRAP_SERVERS_PROPERTY = "event-bridge.kafka.bootstrap.servers";
    public static final String KAFKA_CLIENT_ID_PROPERTY = "event-bridge.kafka.client.id";
    public static final String KAFKA_CLIENT_SECRET_PROPERTY = "event-bridge.kafka.client.secret";
    public static final String KAFKA_SECURITY_PROTOCOL_PROPERTY = "event-bridge.kafka.security.protocol";
    public static final String KAFKA_SASL_MECHANISM_PROPERTY = "event-bridge.kafka.sasl.mechanism";

    @ConfigProperty(name = KAFKA_BOOTSTRAP_SERVERS_PROPERTY)
    String kafkaBootstrapServers;

    @ConfigProperty(name = KAFKA_CLIENT_ID_PROPERTY)
    String kafkaClientId;

    @ConfigProperty(name = KAFKA_CLIENT_SECRET_PROPERTY)
    String kafkaClientSecret;

    @ConfigProperty(name = KAFKA_SECURITY_PROTOCOL_PROPERTY)
    String kafkaSecurityProtocol;

    @ConfigProperty(name = KAFKA_SASL_MECHANISM_PROPERTY)
    String saslMechanism;

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

    @Override
    public String getSaslMechanism() {
        return saslMechanism;
    }
}
