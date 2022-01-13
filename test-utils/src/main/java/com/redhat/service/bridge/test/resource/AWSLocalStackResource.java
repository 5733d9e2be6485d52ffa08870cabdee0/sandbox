package com.redhat.service.bridge.test.resource;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class AWSLocalStackResource implements QuarkusTestResourceLifecycleManager {

    private static final String PROPERTY_CONTAINER_IMAGE_NAME = "container.image.localstack";

    public static final String START_WEB = "0";
    public static final String ENABLED_SERVICES = "secretsmanager";
    public static final String ACCESS_KEY_ID = "test-key";
    public static final String SECRET_ACCESS_KEY = "test-secret";
    public static final String REGION = "us-west-1";

    public static final int PORT = 4566;

    private final GenericContainer localStack = new GenericContainer(DockerImageName.parse(getImageName()));

    public AWSLocalStackResource() {
        localStack.addExposedPort(PORT);
        localStack.withEnv("START_WEB", START_WEB);
        localStack.withEnv("SERVICES", ENABLED_SERVICES);
        localStack.withEnv("AWS_ACCESS_KEY_ID", ACCESS_KEY_ID);
        localStack.withEnv("AWS_SECRET_ACCESS_KEY", SECRET_ACCESS_KEY);
        localStack.waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(5)));
    }

    @Override
    public Map<String, String> start() {
        localStack.start();
        Map<String, String> map = new HashMap<>();
        map.put("event-bridge.secrets-manager.aws.region", REGION);
        map.put("event-bridge.secrets-manager.aws.access-key-id", ACCESS_KEY_ID);
        map.put("event-bridge.secrets-manager.aws.secret-access-key", SECRET_ACCESS_KEY);

        String url = String.format("http://localhost:%s", localStack.getFirstMappedPort());
        map.put("event-bridge.secrets-manager.aws.endpoint-override", url);
        return map;
    }

    @Override
    public void stop() {
        localStack.close();
    }

    private static String getImageName() {
        return Optional.ofNullable(System.getProperty(PROPERTY_CONTAINER_IMAGE_NAME))
                .filter(s -> s.trim().length() > 0)
                .orElseThrow(() -> new IllegalArgumentException(PROPERTY_CONTAINER_IMAGE_NAME + " property should be set in pom.xml"));
    }
}
