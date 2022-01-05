package com.redhat.service.bridge.test.rhoas;

import com.redhat.service.bridge.test.wiremock.AbstractWireMockResourceManager;

public class RhoasMockServerResource extends AbstractWireMockResourceManager {

    public RhoasMockServerResource() {
        super("rhoas-mock-server.url");
    }
}
