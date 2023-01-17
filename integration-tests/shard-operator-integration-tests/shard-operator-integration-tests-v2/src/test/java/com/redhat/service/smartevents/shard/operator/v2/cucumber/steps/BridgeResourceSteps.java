package com.redhat.service.smartevents.shard.operator.v2.cucumber.steps;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.redhat.service.smartevents.integration.tests.context.TestContext;
import com.redhat.service.smartevents.integration.tests.context.resolver.ContextResolver;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridge;

import io.cucumber.java.en.When;

public class BridgeResourceSteps {

    private TestContext context;

    public BridgeResourceSteps(TestContext context) {
        this.context = context;
    }

    @When("^deploy BridgeResource \"([^\"]*)\" using topic \"([^\"]*)\":$")
    public void deployBridgeResource(String bridgeName, String topic, String bridgeResourceYaml) throws InterruptedException {
        String filteredBridgeResourceYaml = ContextResolver.resolveWithScenarioContext(context, bridgeResourceYaml);
        InputStream resourceStream = new ByteArrayInputStream(filteredBridgeResourceYaml.getBytes(StandardCharsets.UTF_8));
        ManagedBridge managedBridge = context.getClient()
                .resources(ManagedBridge.class)
                .inNamespace(context.getNamespace())
                .load(resourceStream)
                .createOrReplace();
    }
}
