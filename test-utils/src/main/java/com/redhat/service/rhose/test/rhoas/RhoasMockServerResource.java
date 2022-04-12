package com.redhat.service.rhose.test.rhoas;

import com.redhat.service.rhose.test.wiremock.AbstractWireMockResourceManager;

public class RhoasMockServerResource extends AbstractWireMockResourceManager {

    public RhoasMockServerResource() {
        super("rhoas-mock-server.url");
    }
}
