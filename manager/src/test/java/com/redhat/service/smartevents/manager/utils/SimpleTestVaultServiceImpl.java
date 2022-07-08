package com.redhat.service.smartevents.manager.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.infra.models.EventBridgeSecret;
import com.redhat.service.smartevents.manager.vault.VaultService;

import io.smallrye.mutiny.Uni;

/**
 * Simple implementation of {@link VaultService} that stores secrets in a {@link HashMap}
 * and can be used in tests without the need of starting up a localstack container.
 * <p>
 * To use it, configure it via {@code QuarkusMock.installMockForType} in a {@link org.junit.jupiter.api.BeforeAll} block:
 *
 * <pre>
 * &#64;BeforeAll
 * public static void setup() {
 *     QuarkusMock.installMockForType(new SimpleTestVaultServiceImpl(), VaultService.class);
 * }
 * </pre>
 */
@ApplicationScoped
public class SimpleTestVaultServiceImpl implements VaultService {

    private final Map<String, EventBridgeSecret> secrets = new HashMap<>();

    @Override
    public Uni<Void> createOrReplace(EventBridgeSecret secret) {
        Objects.requireNonNull(secret);
        Objects.requireNonNull(secret.getId());
        secrets.put(secret.getId(), secret);
        return Uni.createFrom().voidItem();
    }

    @Override
    public Uni<EventBridgeSecret> get(String name) {
        Objects.requireNonNull(name);
        if (!secrets.containsKey(name)) {
            return Uni.createFrom().failure(new ItemNotFoundException("Can't find secret named " + name));
        }
        return Uni.createFrom().item(secrets.get(name));
    }

    @Override
    public Uni<Void> delete(String name) {
        Objects.requireNonNull(name);
        secrets.remove(name);
        return Uni.createFrom().voidItem();
    }
}
