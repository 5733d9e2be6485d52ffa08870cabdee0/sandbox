package com.redhat.service.smartevents.shard.operator;

import com.redhat.service.smartevents.test.wiremock.AbstractWireMockResourceManager;

public class ManagerMockResource extends AbstractWireMockResourceManager {

    public ManagerMockResource() {
        super("event-bridge.manager.url");
    }
}