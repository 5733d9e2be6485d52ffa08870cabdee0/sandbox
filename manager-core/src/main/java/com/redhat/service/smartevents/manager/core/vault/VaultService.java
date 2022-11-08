package com.redhat.service.smartevents.manager.core.vault;

import com.redhat.service.smartevents.infra.core.models.EventBridgeSecret;

import io.smallrye.mutiny.Uni;

public interface VaultService {
    Uni<Void> createOrReplace(EventBridgeSecret secret);

    Uni<EventBridgeSecret> get(String name);

    Uni<Void> delete(String name);
}
