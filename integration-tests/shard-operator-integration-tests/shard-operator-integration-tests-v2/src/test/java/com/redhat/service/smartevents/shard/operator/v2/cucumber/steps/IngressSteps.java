package com.redhat.service.smartevents.shard.operator.v2.cucumber.steps;

import java.time.Duration;

import org.awaitility.Awaitility;

import com.redhat.service.smartevents.integration.tests.context.TestContext;

import io.cucumber.java.en.And;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;

public class IngressSteps {

    private TestContext context;

    public IngressSteps(TestContext context) {
        this.context = context;
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
}
