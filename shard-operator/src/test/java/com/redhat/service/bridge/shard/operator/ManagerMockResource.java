package com.redhat.service.bridge.shard.operator;

import com.redhat.service.bridge.test.wiremock.AbstractWireMockResourceManager;

public class ManagerMockResource extends AbstractWireMockResourceManager {

    public ManagerMockResource() {
        super("event-bridge.manager.url");
    }
}