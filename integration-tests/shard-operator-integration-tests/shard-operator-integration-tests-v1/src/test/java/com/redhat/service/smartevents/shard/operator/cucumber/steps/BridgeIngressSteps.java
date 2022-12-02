package com.redhat.service.smartevents.shard.operator.cucumber.steps;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import com.redhat.service.smartevents.integration.tests.common.TimeUtils;
import com.redhat.service.smartevents.integration.tests.common.Utils;
import com.redhat.service.smartevents.integration.tests.context.ShardContext;
import com.redhat.service.smartevents.shard.operator.core.providers.GlobalConfigurationsConstants;
import com.redhat.service.smartevents.shard.operator.core.utils.LabelsBuilder;
import com.redhat.service.smartevents.shard.operator.v1.resources.BridgeIngress;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;

/**
 * Step definitions related to BridgeIngress
 */
public class BridgeIngressSteps {

    private ShardContext context;

    public BridgeIngressSteps(ShardContext context) {
        this.context = context;
    }

    @When("^deploy BridgeIngress with default secret:$")
    public void deployBridgeIngressWithDefaultSecret(String bridgeIngressYaml) {
        InputStream resourceStream = new ByteArrayInputStream(bridgeIngressYaml.getBytes(StandardCharsets.UTF_8));
        BridgeIngress bridgeIngress = context.getClient().resources(BridgeIngress.class).inNamespace(context.getNamespace()).load(resourceStream).createOrReplace();
        Secret secret = buildDefaultSecret(bridgeIngress);
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
                    return bridgeIngress.getStatus().isConditionTypeTrue(condition);
                },
                String.format("Timeout waiting for BridgeIngress '%s' to be in condition '%s' in namespace '%s'", name, condition, context.getNamespace()));
    }

    private Secret buildDefaultSecret(BridgeIngress bridgeIngress) {
        Map<String, String> data = new HashMap<>();
        data.put(GlobalConfigurationsConstants.KNATIVE_KAFKA_BOOTSTRAP_SERVERS_SECRET, Utils.getSystemProperty("it.shard.kafka.bootstrap.servers"));
        data.put(GlobalConfigurationsConstants.KNATIVE_KAFKA_USER_SECRET, Utils.getSystemProperty("it.shard.kafka.user"));
        data.put(GlobalConfigurationsConstants.KNATIVE_KAFKA_PASSWORD_SECRET, Utils.getSystemProperty("it.shard.kafka.password"));
        data.put(GlobalConfigurationsConstants.KNATIVE_KAFKA_PROTOCOL_SECRET, Utils.getSystemProperty("it.shard.kafka.protocol"));
        data.put(GlobalConfigurationsConstants.KNATIVE_KAFKA_SASL_MECHANISM_SECRET, Utils.getSystemProperty("it.shard.kafka.sasl.mechanism"));
        data.put(GlobalConfigurationsConstants.KNATIVE_KAFKA_TOPIC_NAME_SECRET, Utils.getSystemProperty("it.shard.kafka.topic.name"));

        return new SecretBuilder()
                .withMetadata(
                        new ObjectMetaBuilder()
                                .withLabels(
                                        new LabelsBuilder()
                                                .withManagedByOperator(LabelsBuilder.V1_OPERATOR_NAME)
                                                .withComponent(BridgeIngress.COMPONENT_NAME)
                                                .build())
                                .withNamespace(bridgeIngress.getMetadata().getNamespace())
                                .withName(bridgeIngress.getMetadata().getName())
                                .build())
                .withStringData(data)
                .build();
    }
}
