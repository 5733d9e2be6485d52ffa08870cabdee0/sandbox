package com.redhat.service.bridge.shard.operator.cucumber.steps;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import com.redhat.service.bridge.shard.operator.cucumber.common.Context;
import com.redhat.service.bridge.shard.operator.cucumber.common.TimeUtils;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngress;
import com.redhat.service.bridge.shard.operator.resources.ConditionType;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;

/**
 * Step definitions related to BridgeIngress
 */
public class BridgeIngressSteps {

    private Context context;

    public BridgeIngressSteps(Context context) {
        this.context = context;
    }

    @When("^deploy BridgeIngress:$")
    public BridgeIngress deployBridgeIngress(String bridgeIngressYaml) {
        InputStream resourceStream = new ByteArrayInputStream(bridgeIngressYaml.getBytes(StandardCharsets.UTF_8));
        return context.getClient().resources(BridgeIngress.class).inNamespace(context.getNamespace()).load(resourceStream).createOrReplace();
    }

    @When("^deploy BridgeIngress with default secret:$")
    public void deployBridgeIngressWithDefaultSecret(String bridgeIngressYaml) {
        BridgeIngress bridgeIngress = deployBridgeIngress(bridgeIngressYaml);
        Secret secret = new SecretBuilder()
                .withMetadata(
                        new ObjectMetaBuilder()
                                .withNamespace(bridgeIngress.getMetadata().getNamespace())
                                .withName(bridgeIngress.getMetadata().getName())
                                .build())
                .build();
        context.getClient().secrets().inNamespace(bridgeIngress.getMetadata().getNamespace()).withName(bridgeIngress.getMetadata().getName()).createOrReplace(secret);
    }

    @When("^delete BridgeIngress \"([^\"]*)\"$")
    public void deleteBridgeIngress(String name) {
        Boolean deleted = context.getClient().resources(BridgeIngress.class).inNamespace(context.getNamespace()).withName(name).delete();
        if (deleted == null || !deleted) {
            throw new IllegalArgumentException(String.format("BridgeIngress '%s' not found, cannot be deleted", name));
        }
    }

    @Then("^the BridgeIngress \"([^\"]*)\" exists within (\\d+) (?:minute|minutes)$")
    public void theBridgeIngressExistsWithinMinutes(String name, int timeoutInMinutes) throws TimeoutException {
        TimeUtils.waitForCondition(Duration.ofMinutes(timeoutInMinutes),
                () -> {
                    BridgeIngress bridgeIngress = context.getClient().resources(BridgeIngress.class).inNamespace(context.getNamespace()).withName(name).get();
                    return bridgeIngress != null;
                },
                String.format("Timeout waiting for BridgeIngress '%s' to exist in namespace '%s'", name, context.getNamespace()));
    }

    @Then("^the BridgeIngress \"([^\"]*)\" does not exist within (\\d+) (?:minute|minutes)$")
    public void theBridgeIngressDoesNotExistWithinMinutes(String name, int timeoutInMinutes) throws TimeoutException {
        TimeUtils.waitForCondition(Duration.ofMinutes(timeoutInMinutes),
                () -> {
                    BridgeIngress bridgeIngress = context.getClient().resources(BridgeIngress.class).inNamespace(context.getNamespace()).withName(name).get();
                    return bridgeIngress == null;
                },
                String.format("Timeout waiting for BridgeIngress '%s' to not exist in namespace '%s'", name, context.getNamespace()));
    }

    @Then("^the BridgeIngress \"([^\"]*)\" is in condition \"([^\"]*)\" within (\\d+) (?:minute|minutes)$")
    public void theBridgeIngressIsInPhaseWithinMinutes(String name, String condition, int timeoutInMinutes) throws TimeoutException {
        TimeUtils.waitForCondition(Duration.ofMinutes(timeoutInMinutes),
                () -> {
                    BridgeIngress bridgeIngress = context.getClient().resources(BridgeIngress.class).inNamespace(context.getNamespace()).withName(name).get();
                    if (bridgeIngress == null || bridgeIngress.getStatus() == null) {
                        return false;
                    }
                    return bridgeIngress.getStatus().isConditionTypeTrue(ConditionType.valueOf(condition));
                },
                String.format("Timeout waiting for BridgeIngress '%s' to be in condition '%s' in namespace '%s'", name, condition, context.getNamespace()));
    }
}
