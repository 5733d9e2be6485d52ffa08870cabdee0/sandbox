package com.redhat.service.rhose.shard.operator;

import com.redhat.service.rhose.test.wiremock.AbstractWireMockResourceManager;

public class ManagerMockResource extends AbstractWireMockResourceManager {

    public ManagerMockResource() {
        super("event-bridge.manager.url");
    }
}