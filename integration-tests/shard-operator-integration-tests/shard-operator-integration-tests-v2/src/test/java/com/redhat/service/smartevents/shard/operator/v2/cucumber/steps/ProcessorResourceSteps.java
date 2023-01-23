package com.redhat.service.smartevents.shard.operator.v2.cucumber.steps;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.awaitility.Awaitility;

import com.redhat.service.smartevents.integration.tests.context.TestContext;
import com.redhat.service.smartevents.integration.tests.context.resolver.ContextResolver;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedProcessor;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class ProcessorResourceSteps {

    private TestContext context;

    public ProcessorResourceSteps(TestContext context) {
        this.context = context;
    }

    @When("^deploy ProcessorResource:$")
    public void deployProcessorResource(String processorResourceYaml) throws InterruptedException {
        String filteredProcessorResourceYaml = ContextResolver.resolveWithScenarioContext(context, processorResourceYaml);
        InputStream resourceStream = new ByteArrayInputStream(filteredProcessorResourceYaml.getBytes(StandardCharsets.UTF_8));
        ManagedProcessor managedProcessor = context.getClient()
                .resources(ManagedProcessor.class)
                .inNamespace(context.getNamespace())
                .load(resourceStream)
                .createOrReplace();
    }

    @Then("^the ProcessorResource \"([^\"]*)\" exists within (\\d+) (?:minute|minutes)$")
    public void theProcessorResourceExistsWithinMinutes(String name, int timeoutMinutes){
        Awaitility.await()
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(5))
                .until(() -> {
                    ManagedProcessor processor = context.getClient().resources(ManagedProcessor.class).inNamespace(context.getNamespace()).withName(name).get();
                    return processor != null;
                });
    }

    @Then("^the ProcessorResource \"([^\"]*)\" is in condition \"([^\"]*)\" within (\\d+) (?:minute|minutes)$")
    public void theProcessorResourceIsInConditionWithinMinutes(String name, String condition, int timeoutInMinutes){
        Awaitility.await()
                .atMost(Duration.ofMinutes(timeoutInMinutes))
                .pollInterval(Duration.ofSeconds(5))
                .until(
                        () -> {
                            ManagedProcessor managedProcessor = context.getClient().resources(ManagedProcessor.class).inNamespace(context.getNamespace()).withName(name).get();
                            if (managedProcessor == null || managedProcessor.getStatus() == null) {
                                return false;
                            }
                            return managedProcessor.getStatus().isConditionTypeTrue(condition);
                        });
    }
}
