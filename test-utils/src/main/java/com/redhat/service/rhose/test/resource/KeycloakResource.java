package com.redhat.service.rhose.test.resource;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class KeycloakResource implements QuarkusTestResourceLifecycleManager {

    private static final String PROPERTY_CONTAINER_IMAGE_NAME = "container.image.keycloak";
    private static final String REALM_FILE = "/tmp/realm.json";

    public static final String USER = "admin";
    public static final String PASSWORD = "admin";
    public static final String CLIENT_ID = "event-bridge";
    public static final String CLIENT_SECRET = "secret";
    public static final int PORT = 8080;

    private final GenericContainer keycloak = new GenericContainer(DockerImageName.parse(getImageName()));

    public KeycloakResource() {
        keycloak.addExposedPort(PORT);
        keycloak.withEnv("KEYCLOAK_USER", USER);
        keycloak.withEnv("KEYCLOAK_PASSWORD", PASSWORD);
        keycloak.withEnv("KEYCLOAK_IMPORT", REALM_FILE);
        keycloak.withClasspathResourceMapping("testcontainers/keycloak/realm.json", REALM_FILE, BindMode.READ_ONLY);
        keycloak.waitingFor(Wait.forHttp("/auth").withStartupTimeout(Duration.ofMinutes(5)));
    }

    @Override
    public Map<String, String> start() {
        keycloak.start();
        Map<String, String> map = new HashMap<>();
        String url = String.format("http://localhost:%s/auth/realms/event-bridge-fm", keycloak.getFirstMappedPort());
        map.put("quarkus.oidc.auth-server-url", url);

        // TODO: since there is a specific property for every sso server, this has to be refactored https://issues.redhat.com/browse/MGDOBR-217
        map.put("event-bridge.sso.auth-server-url", url);
        return map;
    }

    @Override
    public void stop() {
        keycloak.close();
    }

    private static String getImageName() {
        return Optional.ofNullable(System.getProperty(PROPERTY_CONTAINER_IMAGE_NAME))
                .filter(s -> s.trim().length() > 0)
                .orElseThrow(() -> new IllegalArgumentException(PROPERTY_CONTAINER_IMAGE_NAME + " property should be set in pom.xml"));
    }
}
