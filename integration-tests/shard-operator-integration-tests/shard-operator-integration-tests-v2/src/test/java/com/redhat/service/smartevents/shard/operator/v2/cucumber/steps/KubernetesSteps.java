package com.redhat.service.smartevents.shard.operator.v2.cucumber.steps;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import com.redhat.service.smartevents.integration.tests.context.TestContext;
import com.redhat.service.smartevents.integration.tests.context.resolver.ContextResolver;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;

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

    @And("^create secret \"([^\"]*)\" with data:$")
    public void createSecret(String name, DataTable values) {
        Map<String, String> secretData = new HashMap<>(values.asMap());
        secretData.replaceAll((key, value) -> ContextResolver.resolveWithScenarioContext(context, value));
        secretData.replaceAll((key, value) -> Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8)));
        Secret secret = new SecretBuilder()
                .withMetadata(
                        new ObjectMetaBuilder()
                                .withNamespace(context.getNamespace())
                                .withName(name)
                                .build())
                .withData(secretData)
                .build();
        context.getClient().secrets().inNamespace(context.getNamespace()).withName(name).createOrReplace(secret);
    }
}
