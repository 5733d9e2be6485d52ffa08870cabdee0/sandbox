package com.redhat.service.bridge.test.resource;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class AppServicesMockResource implements QuarkusTestResourceLifecycleManager {

    public static final String APP_SERVICE_MOCK_SERVICE = "app.services.mock.server";
    private static final String PROPERTY_CONTAINER_IMAGE_NAME = "container.image.app.services.mock";
    public static final int PORT = 8000;

    private GenericContainer appServicesMock;

    public AppServicesMockResource() {
        appServicesMock = new GenericContainer(DockerImageName.parse(getImageName()));
        appServicesMock.addExposedPort(PORT);
        appServicesMock.waitingFor(Wait.forHttp("/").withStartupTimeout(Duration.ofMinutes(1)));
    }

    @Override
    public Map<String, String> start() {
        appServicesMock.start();
        final Map<String, String> map = new HashMap<>();
        map.put(APP_SERVICE_MOCK_SERVICE, "http://" +
                appServicesMock.getHost() + ":" + String.valueOf(appServicesMock.getMappedPort(PORT)));

        return map;
    }

    @Override
    public void stop() {
        appServicesMock.close();
    }

    private static String getImageName() {
        return Optional.ofNullable(System.getProperty(PROPERTY_CONTAINER_IMAGE_NAME))
                .filter(s -> s.trim().length() > 0)
                .orElseThrow(() -> new IllegalArgumentException(PROPERTY_CONTAINER_IMAGE_NAME + " property should be set in pom.xml"));
    }
}
