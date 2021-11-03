package com.redhat.service.bridge.shard.operator.controllers;

import java.time.Duration;

import javax.inject.Inject;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.shard.operator.TestConstants;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngress;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngressSpec;
import com.redhat.service.bridge.shard.operator.utils.KubernetesResourcePatcher;
import com.redhat.service.bridge.shard.operator.utils.LabelsBuilder;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@WithKubernetesTestServer
public class BridgeIngressControllerTest {

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    KubernetesResourcePatcher kubernetesResourcePatcher;

    @BeforeEach
    public void setup(){
        // Kubernetes Server must be cleaned up at startup of every test.
        kubernetesClient.resources(BridgeIngress.class).inAnyNamespace().delete();
    }

    @Test
    void testBridgeIngressReconcileLoop() {
        // Given
        BridgeIngress bridgeIngress = buildBridgeIngress();

        // When
        kubernetesClient.resources(BridgeIngress.class).inNamespace(bridgeIngress.getMetadata().getNamespace()).withName(bridgeIngress.getMetadata().getName()).create(bridgeIngress);
        Awaitility.await()
                .atMost(Duration.ofMinutes(2))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> {
                            kubernetesResourcePatcher.patchReadyDeploymentOrFail(bridgeIngress.getMetadata().getName(), bridgeIngress.getMetadata().getNamespace());
                            Deployment deployment = kubernetesClient.apps().deployments().inNamespace(bridgeIngress.getMetadata().getNamespace()).withName(bridgeIngress.getMetadata().getName()).get();
                            assertThat(deployment).isNotNull();
                            assertThat(deployment.getMetadata().getOwnerReferences().size()).isEqualTo(1);
                            assertThat(deployment.getMetadata().getLabels()).isNotNull();
                            assertThat(deployment.getSpec().getSelector().getMatchLabels().size()).isEqualTo(1);
                            assertThat(deployment.getSpec().getTemplate().getMetadata().getLabels()).isNotNull();
                            assertThat(deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getImage()).isNotNull();
                            assertThat(deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getName()).isNotNull();
                        });

        // Then
        Awaitility.await()
                .atMost(Duration.ofMinutes(2))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> {
                            kubernetesResourcePatcher.patchReadyServiceOrFail(bridgeIngress.getMetadata().getName(), bridgeIngress.getMetadata().getNamespace());
                            Service service = kubernetesClient.services().inNamespace(bridgeIngress.getMetadata().getNamespace()).withName(bridgeIngress.getMetadata().getName()).get();
                            assertThat(service).isNotNull();
                            assertThat(service.getMetadata().getOwnerReferences().size()).isEqualTo(1);
                            assertThat(service.getMetadata().getOwnerReferences().get(0).getKind()).isEqualTo("BridgeIngress");
                            assertThat(service.getMetadata().getLabels()).isNotNull();
                            assertThat(service.getSpec().getSelector().get(LabelsBuilder.INSTANCE_LABEL)).isEqualTo(bridgeIngress.getMetadata().getName());
                        });
    }

    private BridgeIngress buildBridgeIngress() {
        BridgeIngressSpec bridgeIngressSpec = new BridgeIngressSpec();
        bridgeIngressSpec.setId(TestConstants.BRIDGE_ID);
        bridgeIngressSpec.setBridgeName(TestConstants.BRIDGE_NAME);
        bridgeIngressSpec.setImage(TestConstants.INGRESS_IMAGE);
        bridgeIngressSpec.setCustomerId(TestConstants.CUSTOMER_ID);

        BridgeIngress bridgeIngress = new BridgeIngress();
        bridgeIngress.setMetadata(
                new ObjectMetaBuilder()
                        .withName(KubernetesResourceUtil.sanitizeName(TestConstants.BRIDGE_ID))
                        .withNamespace(KubernetesResourceUtil.sanitizeName(TestConstants.CUSTOMER_ID))
                        .withLabels(new LabelsBuilder()
                                            .withCustomerId(TestConstants.CUSTOMER_ID)
                                            .withComponent("ingress")
                                            .build())
                        .build());
        bridgeIngress.setSpec(bridgeIngressSpec);

        return bridgeIngress;
    }
}
