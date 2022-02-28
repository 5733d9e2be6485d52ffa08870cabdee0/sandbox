package com.redhat.service.bridge.integration.tests.steps;

import com.redhat.service.bridge.integration.tests.common.BridgeUtils;
import com.redhat.service.bridge.integration.tests.context.TestContext;

import io.cucumber.java.en.Given;

public class ManagerSteps {

    private TestContext context;

    public ManagerSteps(TestContext context) {
        this.context = context;
    }

    @Given("^authentication is done against Manager$")
    public void authenticationIsDoneAgainstManager() {
        context.setManagerToken(BridgeUtils.retrieveBridgeToken());
    }
}
