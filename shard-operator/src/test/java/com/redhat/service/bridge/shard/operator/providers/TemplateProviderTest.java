package com.redhat.service.bridge.shard.operator.providers;

import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.apps.Deployment;

import static org.assertj.core.api.Assertions.assertThat;

public class TemplateProviderTest {

    @Test
    public void bridgeIngressTemplateIsProvided() {
        TemplateProvider templateProvider = new TemplateProviderImpl();
        Deployment deployment = templateProvider.loadIngressDeploymentTemplate();

        assertThat(deployment.getMetadata().getOwnerReferences().size()).isEqualTo(1);
        assertThat(deployment.getMetadata().getLabels().size()).isEqualTo(3);
        assertThat(deployment.getSpec().getReplicas()).isEqualTo(1);
    }
}
