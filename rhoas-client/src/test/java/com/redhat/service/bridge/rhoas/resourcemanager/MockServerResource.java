package com.redhat.service.bridge.rhoas.resourcemanager;

import com.redhat.service.bridge.test.wiremock.AbstractWireMockResourceManager;

public class MockServerResource extends AbstractWireMockResourceManager {

    public MockServerResource() {
        super("mock-server.url");
    }
}
