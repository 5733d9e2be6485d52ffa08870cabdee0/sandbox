package com.redhat.service.smartevents.test.rhoas;

import com.redhat.service.smartevents.test.wiremock.AbstractWireMockResourceManager;

public class RhoasMockServerResource extends AbstractWireMockResourceManager {

    public RhoasMockServerResource() {
        super("rhoas-mock-server.url");
    }
}
