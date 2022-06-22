package com.redhat.service.smartevents.manager;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.manager.api.models.requests.BridgeRequest;

/**
 * A clone of {@see BridgeRequest} however it has setters for the properties, useful in tests.
 */
public class BridgeRequestForTests extends BridgeRequest {

    public BridgeRequestForTests(String name) {
        super(name);
    }

    public BridgeRequestForTests(String name, Action action) {
        super(name, action);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setErrorHandler(Action errorHandler) {
        this.errorHandler = errorHandler;
    }

}
