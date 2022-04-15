package com.redhat.service.smartevents.shard.operator.cucumber.steps;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import com.redhat.service.smartevents.shard.operator.cucumber.common.Context;
import com.redhat.service.smartevents.shard.operator.cucumber.common.TimeUtils;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.resources.ConditionType;
import com.redhat.service.smartevents.shard.operator.utils.LabelsBuilder;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;

/*
 * Step definitions related to BridgeExecutor
 */
public class BridgeExecutorSteps {

    private Context context;

    public BridgeExecutorSteps(Context context) {
        this.context = context;
    }

    @When("^deploy BridgeExecutor with default secret:$")
    public void deployBridgeIngressWithDefaultSecrets(String bridgeExecutorYaml) {
        InputStream resourceStream = new ByteArrayInputStream(bridgeExecutorYaml.getBytes(StandardCharsets.UTF_8));
        BridgeExecutor bridgeExecutor = context.getClient().resources(BridgeExecutor.class).inNamespace(context.getNamespace()).load(resourceStream).create();
        Secret secret = new SecretBuilder()
                .withMetadata(
                        new ObjectMetaBuilder()
                                .withLabels(
                                        new LabelsBuilder()
                                                .withManagedByOperator()
                                                .withComponent(BridgeExecutor.COMPONENT_NAME)
                                                .build())
                                .withNamespace(bridgeExecutor.getMetadata().getNamespace())
                                .withName(bridgeExecutor.getMetadata().getName())
                                .build())
                .build();
        context.getClient().secrets().inNamespace(bridgeExecutor.getMetadata().getNamespace()).withName(bridgeExecutor.getMetadata().getName()).createOrReplace(secret);
    }

    @When("^delete BridgeExecutor \"([^\"]*)\"$")
    public void deleteBridgeExecutor(String name) {
        Boolean deleted = context.getClient().resources(BridgeExecutor.class).inNamespace(context.getNamespace()).withName(name).delete();
        if (deleted == null || !deleted) {
            throw new IllegalArgumentException(String.format("BridgeExecutor '%s' not found, cannot be deleted", name));
        }
    }

    @Then("^the BridgeExecutor \"([^\"]*)\" exists within (\\d+) (?:minute|minutes)$")
    public void theBridgeExecutorExistsWithinMinutes(String name, int timeoutInMinutes) throws TimeoutException {
        TimeUtils.waitForCondition(Duration.ofMinutes(timeoutInMinutes),
                () -> {
                    BridgeExecutor bridgeExecutor = context.getClient().resources(BridgeExecutor.class).inNamespace(context.getNamespace()).withName(name).get();
                    return bridgeExecutor != null;
                },
                String.format("Timeout waiting for BridgeExecutor '%s' to exist in namespace '%s'", name, context.getNamespace()));
    }

    @Then("^the BridgeExecutor \"([^\"]*)\" does not exists within (\\d+) (?:minute|minutes)$")
    public void theBridgeExecutorDoesNotExistsWithinMinutes(String name, int timeoutInMinutes) throws TimeoutException {
        TimeUtils.waitForCondition(Duration.ofMinutes(timeoutInMinutes),
                () -> {
                    BridgeExecutor bridgeExecutor = context.getClient().resources(BridgeExecutor.class).inNamespace(context.getNamespace()).withName(name).get();
                    return bridgeExecutor == null;
                },
                String.format("Timeout waiting for BridgeExecutor '%s' to not exist in namespace '%s'", name, context.getNamespace()));
    }

    @Then("^the BridgeExecutor \"([^\"]*)\" is in condition \"([^\"]*)\" within (\\d+) (?:minute|minutes)$")
    public void theBridgeExecutorIsinConditionWithinMinutes(String name, String condition, int timeoutInMinutes) throws TimeoutException {
        TimeUtils.waitForCondition(Duration.ofMinutes(timeoutInMinutes),
                () -> {
                    BridgeExecutor bridgeExecutor = context.getClient().resources(BridgeExecutor.class).inNamespace(context.getNamespace()).withName(name).get();
                    if (bridgeExecutor == null || bridgeExecutor.getStatus() == null) {
                        return false;
                    }
                    return bridgeExecutor.getStatus().isConditionTypeTrue(ConditionType.valueOf(condition));
                },
                String.format("Timeout waiting for BridgeExecutor '%s' to be in condition '%s' in namespace '%s'", name, condition, context.getNamespace()));
    }
}
