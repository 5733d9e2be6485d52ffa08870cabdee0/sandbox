package com.redhat.service.smartevents.manager.vault;

import com.redhat.service.smartevents.infra.models.VaultSecret;

import io.smallrye.mutiny.Uni;

public interface VaultService {
    Uni<Void> createOrReplace(VaultSecret secret);

    Uni<VaultSecret> get(String name);

    Uni<Void> delete(String name);
}
