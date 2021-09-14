package com.redhat.service.bridge.runner.it;

import java.util.HashMap;
import java.util.Map;

import org.testcontainers.containers.PostgreSQLContainer;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class PostgresResource implements QuarkusTestResourceLifecycleManager {

    private final PostgreSQLContainer postgres = new PostgreSQLContainer<>("postgres:10.5")
            .withDatabaseName("event-bridge-test")
            .withUsername("event-bridge-test")
            .withPassword("event-bridge-test")
            .withExposedPorts(5432)
            .withCreateContainerCmdModifier(cmd -> cmd.withHostName("localhost"));

    @Override
    public Map<String, String> start() {
        postgres.start();

        Map<String, String> config = new HashMap<>();
        config.put("EVENT_BRIDGE_DB_USERNAME", postgres.getUsername());
        config.put("EVENT_BRIDGE_DB_PASSWORD", postgres.getPassword());
        config.put("EVENT_BRIDGE_DB_HOST", postgres.getHost());
        config.put("EVENT_BRIDGE_DB_PORT", String.valueOf(postgres.getFirstMappedPort()));
        config.put("EVENT_BRIDGE_DB_SCHEMA", postgres.getDatabaseName());
        return config;
    }

    @Override
    public void stop() {
        postgres.close();
    }
}
