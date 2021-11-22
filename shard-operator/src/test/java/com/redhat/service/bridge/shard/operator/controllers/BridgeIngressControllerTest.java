package com.redhat.service.bridge.shard.operator.controllers;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.shard.operator.TestConstants;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngress;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@WithOpenShiftTestServer
public class BridgeIngressControllerTest {

    @Inject
    BridgeIngressController bridgeIngressController;

    @Inject
    KubernetesClient kubernetesClient;

    @BeforeEach
    void setup() {
        kubernetesClient.resources(BridgeIngress.class).inAnyNamespace().delete();
    }

    @Test
    void testCreateNewBridgeIngress() {
        // Given
        BridgeIngress bridgeIngress = buildBridgeIngress();

        // When
        UpdateControl<BridgeIngress> updateControl = bridgeIngressController.createOrUpdateResource(bridgeIngress, null);

        // Then
        assertThat(updateControl.isUpdateStatusSubResource()).isTrue();
    }

    @Test
    void testBridgeIngressDeployment() {
        // Given
        BridgeIngress bridgeIngress = buildBridgeIngress();

        // When
        bridgeIngressController.createOrUpdateResource(bridgeIngress, null);

        // Then
        Deployment deployment = kubernetesClient.apps().deployments().inNamespace(bridgeIngress.getMetadata().getNamespace()).withName(bridgeIngress.getMetadata().getName()).get();
        assertThat(deployment).isNotNull();
        assertThat(deployment.getMetadata().getOwnerReferences().size()).isEqualTo(1);
        assertThat(deployment.getMetadata().getLabels()).isNotNull();
        assertThat(deployment.getSpec().getSelector().getMatchLabels().size()).isEqualTo(1);
        assertThat(deployment.getSpec().getTemplate().getMetadata().getLabels()).isNotNull();
        assertThat(deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getImage()).isNotNull();
        assertThat(deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getName()).isNotNull();
    }

    private BridgeIngress buildBridgeIngress() {
        return BridgeIngress.fromBuilder()
                .withBridgeId(TestConstants.BRIDGE_ID)
                .withBridgeName(TestConstants.BRIDGE_NAME)
                .withImageName(TestConstants.INGRESS_IMAGE)
                .withCustomerId(TestConstants.CUSTOMER_ID)
                .withNamespace(KubernetesResourceUtil.sanitizeName(TestConstants.CUSTOMER_ID))
                .build();
    }
}
