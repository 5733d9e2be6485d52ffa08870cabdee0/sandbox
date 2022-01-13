package com.redhat.service.bridge.manager.vault;

import com.redhat.service.bridge.infra.models.EventBridgeSecret;

public interface VaultService {
    void createOrReplace(EventBridgeSecret secret);

    EventBridgeSecret get(String name);

    String delete(String name);
}