package com.redhat.service.bridge.manager.vault;

import com.redhat.service.bridge.infra.models.EventBridgeSecret;

import io.smallrye.mutiny.Uni;

public interface VaultService {
    Uni<Void> createOrReplace(EventBridgeSecret secret);

    Uni<EventBridgeSecret> get(String name);

    Uni<Void> delete(String name);
}
