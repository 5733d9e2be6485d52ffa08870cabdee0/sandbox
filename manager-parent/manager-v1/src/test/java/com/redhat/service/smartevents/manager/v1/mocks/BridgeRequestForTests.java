package com.redhat.service.smartevents.manager.v1.mocks;

import com.redhat.service.smartevents.infra.v1.api.models.gateways.Action;
import com.redhat.service.smartevents.manager.v1.api.models.requests.BridgeRequest;

/**
 * A clone of {@see BridgeRequest} however it has setters for the properties, useful in tests.
 */
public class BridgeRequestForTests extends BridgeRequest {

    public BridgeRequestForTests(String name, String cloudProvider, String region) {
        super(name, cloudProvider, region);
    }

    public BridgeRequestForTests(String name, String cloudProvider, String region, Action action) {
        super(name, cloudProvider, region, action);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCloudProvider(String cloudProvider) {
        this.cloudProvider = cloudProvider;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void setErrorHandler(Action errorHandler) {
        this.errorHandler = errorHandler;
    }

}
