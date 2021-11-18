package com.redhat.service.bridge.shard.operator.controllers;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.shard.operator.TestConstants;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngress;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngressSpec;
import com.redhat.service.bridge.shard.operator.utils.KubernetesResourcePatcher;
import com.redhat.service.bridge.shard.operator.utils.LabelsBuilder;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
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

    @Inject
    KubernetesResourcePatcher kubernetesResourcePatcher;

    @BeforeEach
    void setup() {
        kubernetesResourcePatcher.cleanUp();
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
        BridgeIngressSpec bridgeIngressSpec = new BridgeIngressSpec();
        bridgeIngressSpec.setId(TestConstants.BRIDGE_ID);
        bridgeIngressSpec.setBridgeName(TestConstants.BRIDGE_NAME);
        bridgeIngressSpec.setImage(TestConstants.INGRESS_IMAGE);
        bridgeIngressSpec.setCustomerId(TestConstants.CUSTOMER_ID);

        BridgeIngress bridgeIngress = new BridgeIngress();
        bridgeIngress.setMetadata(
                new ObjectMetaBuilder()
                        .withName(BridgeIngress.buildResourceName(TestConstants.BRIDGE_ID))
                        .withNamespace(KubernetesResourceUtil.sanitizeName(TestConstants.CUSTOMER_ID))
                        .withLabels(new LabelsBuilder()
                                .withCustomerId(TestConstants.CUSTOMER_ID)
                                .withComponent(BridgeIngress.COMPONENT_NAME)
                                .build())
                        .build());
        bridgeIngress.setSpec(bridgeIngressSpec);

        return bridgeIngress;
    }
}
