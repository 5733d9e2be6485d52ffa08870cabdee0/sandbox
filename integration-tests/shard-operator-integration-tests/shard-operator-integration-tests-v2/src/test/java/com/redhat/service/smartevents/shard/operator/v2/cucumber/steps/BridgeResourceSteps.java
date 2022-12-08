package com.redhat.service.smartevents.shard.operator.v2.cucumber.steps;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.awaitility.Awaitility;

import com.redhat.service.smartevents.integration.tests.context.TestContext;
import com.redhat.service.smartevents.integration.tests.context.resolver.ContextResolver;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridge;

import io.cucumber.java.en.And;
import io.cucumber.java.en.When;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;

public class BridgeResourceSteps {

    private TestContext context;

    public BridgeResourceSteps(TestContext context) {
        this.context = context;
    }

    @When("^deploy BridgeResource \"([^\"]*)\" using topic \"([^\"]*)\":$")
    public void deployBridgeResource(String bridgeName, String topic, String bridgeResourceYaml) throws InterruptedException {
        Map<String, String> secretData = new HashMap<>();
        addSecretPair(secretData, "bootstrap.servers", System.getProperty("it.shard.kafka.bootstrap.servers"));
        addSecretPair(secretData, "user", System.getProperty("it.shard.kafka.user"));
        addSecretPair(secretData, "password", System.getProperty("it.shard.kafka.password"));
        addSecretPair(secretData, "protocol", "SASL_SSL");
        addSecretPair(secretData, "topic.name", context.getKafkaTopic(topic));
        addSecretPair(secretData, "sasl.mechanism", "PLAIN");
        Secret secret = new SecretBuilder()
                .withMetadata(
                        new ObjectMetaBuilder()
                                .withNamespace(context.getNamespace())
                                .withName(bridgeName)
                                .build())
                .withData(secretData)
                .build();
        context.getClient().secrets().inNamespace(context.getNamespace()).withName(bridgeName).createOrReplace(secret);

        String filteredBridgeResourceYaml = ContextResolver.resolveWithScenarioContext(context, bridgeResourceYaml);
        InputStream resourceStream = new ByteArrayInputStream(filteredBridgeResourceYaml.getBytes(StandardCharsets.UTF_8));
        ManagedBridge managedBridge = context.getClient()
                .resources(ManagedBridge.class)
                .inNamespace(context.getNamespace())
                .load(resourceStream)
                .createOrReplace();
    }

    @And("^the Ingress \"([^\"]*)\" is available within (\\d+) (?:minute|minutes)$")
    public void ingressIsAvailableWithinMinutes(String testBridgeName, int timeoutMinutes) {
        Awaitility.await()
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(5))
                .until(() -> {
                    Ingress ingress = context.getClient().network().v1().ingresses().inNamespace("istio-system").withName(testBridgeName).get();
                    return ingress != null;
                });
    }

    private static void addSecretPair(Map<String, String> data, String key, String value) {
        String base64Value = Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
        data.put(key, base64Value);
    }
}
