package com.redhat.service.smartevents.integration.tests.steps;

import com.redhat.service.smartevents.integration.tests.common.BridgeUtils;
import com.redhat.service.smartevents.integration.tests.context.TestContext;

import io.cucumber.java.en.Given;

public class ManagerSteps {

    private TestContext context;

    public ManagerSteps(TestContext context) {
        this.context = context;
    }

    @Given("^authenticate against Manager$")
    public void authenticateAgainstManager() {
        context.setManagerToken(BridgeUtils.retrieveBridgeToken());
    }

    @Given("^logout of Manager$")
    public void logoutOfManager() {
        context.setManagerToken(null);
    }
}
