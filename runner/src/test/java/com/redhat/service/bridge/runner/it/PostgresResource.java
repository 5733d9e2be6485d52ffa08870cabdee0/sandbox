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
            .withDatabaseName("hibernate_orm_test")
            .withUsername("hibernate_orm_test")
            .withPassword("hibernate_orm_test")
            .withExposedPorts(5432)
            .withCreateContainerCmdModifier(cmd -> {
                cmd
                        .withHostName("localhost")
                        .withPortBindings(new PortBinding(Ports.Binding.bindPort(5432), new ExposedPort(5432)));
            });

    @Override
    public Map<String, String> start() {
        System.out.println("starting");
        postgres.start();
        System.out.println("started");
        Map<String, String> config = new HashMap<>();

        config.put("EVENT_BRIDGE_DB_USERNAME", postgres.getUsername());
        config.put("EVENT_BRIDGE_DB_PASSWORD", postgres.getPassword());
        config.put("EVENT_BRIDGE_DB_HOST", postgres.getHost());
        config.put("EVENT_BRIDGE_DB_PORT", "5432");
        config.put("EVENT_BRIDGE_DB_SCHEMA", postgres.getDatabaseName());
        return config;
    }

    @Override
    public void stop() {
        postgres.close();
    }
}
