package com.redhat.service.smartevents.shard.operator.v2.cucumber.steps;

import com.redhat.service.smartevents.integration.tests.context.TestContext;

import io.cucumber.java.en.Given;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;

public class KubernetesSteps {
    private TestContext context;

    public KubernetesSteps(TestContext context) {
        this.context = context;
    }

    @Given("^create Namespace$")
    public void createNamespace() {
        context.getClient().namespaces().createOrReplace(
                new NamespaceBuilder()
                        .withNewMetadata()
                        .withName(context.getNamespace())
                        .endMetadata()
                        .build());
    }
}
