package com.redhat.service.bridge.processor.actions.kafkatopic;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;

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
}
