package com.redhat.service.bridge.shard.operator.controllers;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.shard.operator.TestSupport;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngress;
import com.redhat.service.bridge.shard.operator.resources.ConditionReason;
import com.redhat.service.bridge.shard.operator.resources.ConditionStatus;
import com.redhat.service.bridge.shard.operator.resources.ConditionType;
import com.redhat.service.bridge.shard.operator.resources.knative.KnativeBroker;
import com.redhat.service.bridge.shard.operator.utils.KubernetesResourcePatcher;
import com.redhat.service.bridge.test.resource.KeycloakResource;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@WithOpenShiftTestServer
@QuarkusTestResource(value = KeycloakResource.class, restrictToAnnotatedClass = true)
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
    void testCreateNewBridgeIngressWithoutSecrets() {
        // Given
        BridgeIngress bridgeIngress = buildBridgeIngress();

        // When
        UpdateControl<BridgeIngress> updateControl = bridgeIngressController.reconcile(bridgeIngress, null);

        // Then
        assertThat(updateControl.isNoUpdate()).isTrue();
    }

    @Test
    void testCreateNewBridgeIngress() {
        // Given
        BridgeIngress bridgeIngress = buildBridgeIngress();
        deployBridgeIngressSecret(bridgeIngress);

        // When
        UpdateControl<BridgeIngress> updateControl = bridgeIngressController.reconcile(bridgeIngress, null);

        // Then
        assertThat(updateControl.isUpdateStatus()).isTrue();
        assertThat(bridgeIngress.getStatus()).isNotNull();
        assertThat(bridgeIngress.getStatus().isReady()).isFalse();
        assertThat(bridgeIngress.getStatus().getConditionByType(ConditionType.Augmentation)).isPresent().hasValueSatisfying(c -> {
            assertThat(c.getStatus()).isEqualTo(ConditionStatus.False);
        });
        assertThat(bridgeIngress.getStatus().getConditionByType(ConditionType.Ready)).isPresent().hasValueSatisfying(c -> {
            assertThat(c.getStatus()).isEqualTo(ConditionStatus.False);
            assertThat(c.getReason()).isEqualTo(ConditionReason.KnativeBrokerNotReady);
        });
    }

    @Test
    void testBridgeIngressKnativeBroker() {
        // Given
        BridgeIngress bridgeIngress = buildBridgeIngress();
        deployBridgeIngressSecret(bridgeIngress);

        // When
        bridgeIngressController.reconcile(bridgeIngress, null);

        // Then
        KnativeBroker knativeBroker = kubernetesClient
                .resources(KnativeBroker.class)
                .inNamespace(bridgeIngress.getMetadata().getNamespace())
                .withName(bridgeIngress.getMetadata().getName())
                .get();

        assertThat(knativeBroker).isNotNull();
        assertThat(knativeBroker.getMetadata().getOwnerReferences().size()).isEqualTo(1);
        assertThat(knativeBroker.getMetadata().getLabels()).isNotNull();
        assertThat(knativeBroker.getSpec().getConfig().getApiVersion()).isNotNull();
        assertThat(knativeBroker.getSpec().getConfig().getName()).isNotNull();
        assertThat(knativeBroker.getSpec().getConfig().getNamespace()).isNotNull();
        assertThat(knativeBroker.getSpec().getConfig().getKind()).isNotNull();
    }

    private void deployBridgeIngressSecret(BridgeIngress bridgeIngress) {
        Secret secret = new SecretBuilder()
                .withMetadata(
                        new ObjectMetaBuilder()
                                .withNamespace(bridgeIngress.getMetadata().getNamespace())
                                .withName(bridgeIngress.getMetadata().getName())
                                .build())
                .build();
        kubernetesClient
                .secrets()
                .inNamespace(bridgeIngress.getMetadata().getNamespace())
                .withName(bridgeIngress.getMetadata().getName())
                .createOrReplace(secret);
    }

    private BridgeIngress buildBridgeIngress() {
        return BridgeIngress.fromBuilder()
                .withBridgeId(TestSupport.BRIDGE_ID)
                .withBridgeName(TestSupport.BRIDGE_NAME)
                .withImageName(TestSupport.INGRESS_IMAGE)
                .withCustomerId(TestSupport.CUSTOMER_ID)
                .withNamespace(KubernetesResourceUtil.sanitizeName(TestSupport.CUSTOMER_ID))
                .build();
    }
}
