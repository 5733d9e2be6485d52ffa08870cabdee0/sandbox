package com.redhat.service.smartevents.shard.operator.v2.cucumber.steps;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import org.awaitility.Awaitility;

import com.redhat.service.smartevents.integration.tests.context.TestContext;
import com.redhat.service.smartevents.integration.tests.context.resolver.ContextResolver;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedProcessor;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;

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
    public void theProcessorResourceExistsWithinMinutes(String name, int timeoutMinutes) throws TimeoutException {
        Awaitility.await()
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(5))
                .until(() -> {
                    ManagedProcessor processor = context.getClient().resources(ManagedProcessor.class).inNamespace(context.getNamespace()).withName(name).get();
                    return processor != null;
                });
    }

    @Then("^the Deployment \"([^\"]*)\" is ready within (\\d+) (?:minute|minutes)$")
    public void theDeploymentIsInStateWithinMinutes(String name, int timeoutInMinutes) throws TimeoutException {
        Awaitility.await()
                .atMost(Duration.ofMinutes(timeoutInMinutes))
                .pollInterval(Duration.ofSeconds(5))
                .until(
                        () -> {
                            Deployment deployment = context.getClient().apps().deployments().inNamespace(context.getNamespace()).withName(name).get();
                            if (deployment == null) {
                                return false;
                            }
                            //I am not sure if this code is valid for v2: deployment.getStatus().getConditions().stream().anyMatch(d -> d.getType().equals("Available") && d.getStatus().equals("True"))
                            //Th PR of MGDOBR-1248 contains this implementation of the `isReadyV2` function in CustomResourceStatus: conditions.stream().allMatch(c -> ConditionStatus.True.equals(c.getStatus()))
                            //return deployment.getStatus().getConditions().stream().allMatch(d -> d.getStatus().equals("True"));
                            return deployment.getStatus().getConditions().stream().anyMatch(d -> d.getType().equals("Available") && d.getStatus().equals("True"));
                        });
    }

    @Then("^the Service \"([^\"]*)\" exists within (\\d+) (?:minute|minutes)$")
    public void theServiceExistsWithinMinutes(String name, int timeoutInMinutes) throws TimeoutException {
        Awaitility.await()
                .atMost(Duration.ofMinutes(timeoutInMinutes))
                .pollInterval(Duration.ofSeconds(5))
                .until(
                        () -> {
                            Service service = context.getClient().services().inNamespace(context.getNamespace()).withName(name).get();
                            return service != null;
                        });
    }

    @Then("^the BridgeExecutor \"([^\"]*)\" is in condition \"([^\"]*)\" within (\\d+) (?:minute|minutes)$")
    public void theBridgeExecutorIsInConditionWithinMinutes(String name, String condition, int timeoutInMinutes) throws TimeoutException {
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
