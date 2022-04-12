package com.redhat.service.rhose.manager.vault;

import com.redhat.service.rhose.infra.models.EventBridgeSecret;

import io.smallrye.mutiny.Uni;

public interface VaultService {
    Uni<Void> createOrReplace(EventBridgeSecret secret);

    Uni<EventBridgeSecret> get(String name);

    Uni<Void> delete(String name);
}
