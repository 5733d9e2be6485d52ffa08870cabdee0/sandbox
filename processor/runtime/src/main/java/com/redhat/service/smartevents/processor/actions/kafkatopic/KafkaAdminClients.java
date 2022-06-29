package com.redhat.service.smartevents.processor.actions.kafkatopic;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;

import io.smallrye.common.annotation.Identifier;

@ApplicationScoped
public class KafkaAdminClients {

    @Inject
    @Identifier("default-kafka-broker")
    Map<String, Object> defaultConfig;

    @Inject
    @Identifier("actions-out")
    Map<String, Object> outboundConfig;

    /*
     * Taken from: https://quarkus.io/guides/kafka#kafka-bare-clients
     */
    @Produces
    @ApplicationScoped
    @Named("defaultAdminClient")
    AdminClient adminClientDefault() {
        return createAdminClient(defaultConfig);
    }

    @Produces
    @ApplicationScoped
    @Named("outboundAdminClient")
    AdminClient adminClientOutbound() {
        return createAdminClient(outboundConfig);
    }

    private AdminClient createAdminClient(Map<String, Object> config) {
        Map<String, Object> copy = new HashMap<>();
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            if (AdminClientConfig.configNames().contains(entry.getKey())) {
                copy.put(entry.getKey(), entry.getValue());
            }
        }
        return AdminClient.create(copy);
    }
}
