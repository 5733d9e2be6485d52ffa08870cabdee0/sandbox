package com.redhat.service.smartevents.shard.operator.controllers;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.redhat.service.smartevents.shard.operator.TestSupport;
import com.redhat.service.smartevents.shard.operator.providers.GlobalConfigurationsConstants;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.resources.ConditionReasonConstants;
import com.redhat.service.smartevents.shard.operator.resources.ConditionStatus;
import com.redhat.service.smartevents.shard.operator.resources.ConditionTypeConstants;
import com.redhat.service.smartevents.shard.operator.resources.knative.KnativeBroker;
import com.redhat.service.smartevents.shard.operator.utils.KubernetesResourcePatcher;
import com.redhat.service.smartevents.test.resource.KeycloakResource;

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
    void testCreateNewBridgeIngress() throws JsonProcessingException {
        // Given
        BridgeIngress bridgeIngress = buildBridgeIngress();
        deployBridgeIngressSecret(bridgeIngress);

        // When
        UpdateControl<BridgeIngress> updateControl1 = bridgeIngressController.reconcile(bridgeIngress, null);

        // Then (First reconciliation marks the CRD as being reconciled)
        assertUpdateControl(bridgeIngress, updateControl1, ConditionReasonConstants.RECONCILIATION_PROGRESSING);

        // When
        UpdateControl<BridgeIngress> updateControl2 = bridgeIngressController.reconcile(bridgeIngress, null);

        // Then (Second reconciliation performs updates)
        assertUpdateControl(bridgeIngress, updateControl2, ConditionReasonConstants.KNATIVE_BROKER_NOT_READY);
    }

    private void assertUpdateControl(BridgeIngress bridgeIngress,
            UpdateControl<BridgeIngress> updateControl,
            String reason) {
        assertThat(updateControl.isUpdateStatus()).isTrue();
        assertThat(bridgeIngress.getStatus()).isNotNull();
        assertThat(bridgeIngress.getStatus().isReady()).isFalse();
        assertThat(bridgeIngress.getStatus().getConditionByType(ConditionTypeConstants.READY)).isPresent().hasValueSatisfying(c -> {
            assertThat(c.getStatus()).isEqualTo(ConditionStatus.False);
        });
        assertThat(bridgeIngress.getStatus().getConditionByType(ConditionTypeConstants.AUGMENTATION)).isPresent().hasValueSatisfying(c -> {
            assertThat(c.getStatus()).isEqualTo(ConditionStatus.True);
            assertThat(c.getReason()).isEqualTo(reason);
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
        Map<String, String> data = new HashMap<>();
        data.put(GlobalConfigurationsConstants.KNATIVE_KAFKA_BOOTSTRAP_SERVERS_SECRET, Base64.getEncoder().encodeToString("bootstrap.servers".getBytes(StandardCharsets.UTF_8)));
        data.put(GlobalConfigurationsConstants.KNATIVE_KAFKA_TOPIC_NAME_SECRET, Base64.getEncoder().encodeToString("topic".getBytes(StandardCharsets.UTF_8)));

        Secret secret = new SecretBuilder()
                .withMetadata(
                        new ObjectMetaBuilder()
                                .withNamespace(bridgeIngress.getMetadata().getNamespace())
                                .withName(bridgeIngress.getMetadata().getName())
                                .build())
                .withData(data)
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
                .withCustomerId(TestSupport.CUSTOMER_ID)
                .withNamespace(KubernetesResourceUtil.sanitizeName(TestSupport.CUSTOMER_ID))
                .build();
    }
}
