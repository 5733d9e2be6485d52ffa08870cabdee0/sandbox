package com.redhat.service.smartevents.processor.actions.kafkatopic;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;

import io.smallrye.common.annotation.Identifier;

@ApplicationScoped
public class KafkaClients {

    @Inject
    @Identifier("default-kafka-broker")
    Map<String, Object> config;

    /*
     * Taken from: https://quarkus.io/guides/kafka#kafka-bare-clients
     */
    @Produces
    @ApplicationScoped
    AdminClient adminClient() {
        Map<String, Object> copy = new HashMap<>();
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            if (AdminClientConfig.configNames().contains(entry.getKey())) {
                copy.put(entry.getKey(), entry.getValue());
            }
        }
        return AdminClient.create(copy);
    }

    @Inject
    ProcessorDTO processorDTO;

    @ConfigProperty(name = "KAFKA_ERROR_TOPIC")
    String kafkaErrorTopic;

    // See https://quarkus.io/guides/kafka#kafka-configuration-resolution
    // Attribute values are resolved as follows:
    // the attribute is set directly on the channel configuration (mp.messaging.incoming.my-channel.attribute=value),
    // if not set, the connector looks for a Map with the channel name or the configured kafka-configuration (if set) and the value is retrieved from that Map
    // If the resolved Map does not contain the value the default Map is used (exposed with the default-kafka-broker name)
    //
    // Therefore if this map doesn't have the value, the default kafka client is used
    @Produces
    @ApplicationScoped
    @Identifier("actions-out")
    Map<String, Object> outgoing() {
        if(processorDTO.getDefinition() == null || processorDTO.getDefinition().getResolvedAction() == null) {
            return Collections.emptyMap();
        }

        Action action = processorDTO.getDefinition().getResolvedAction();

        String brokerUrl = action.getParameter(KafkaTopicAction.BROKER_URL);
        String clientId = action.getParameter(KafkaTopicAction.CLIENT_ID);
        String clientSecret = action.getParameter(KafkaTopicAction.CLIENT_SECRET);

        return Map.ofEntries(
                Map.entry("bootstrap.servers", brokerUrl),
                Map.entry("asl.mechanism", "PLAIN"),
                Map.entry("security.protocol", "SASL_SSL"),
                Map.entry("dead-letter-queue.topic", kafkaErrorTopic),
                Map.entry("sasl.jaas.config",
                        String.format("org.apache.kafka.common.security.plain.PlainLoginModule required username=%s password=%s;",
                                clientId, clientSecret)));
    }
}
