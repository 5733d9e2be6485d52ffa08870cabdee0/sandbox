package com.redhat.service.smartevents.shard.operator.v1.controllers;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.shard.operator.core.resources.ConditionStatus;
import com.redhat.service.smartevents.shard.operator.core.resources.ConditionTypeConstants;
import com.redhat.service.smartevents.shard.operator.v1.TestSupport;
import com.redhat.service.smartevents.shard.operator.v1.providers.GlobalConfigurationsConstants;
import com.redhat.service.smartevents.shard.operator.v1.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.v1.resources.BridgeIngressStatus;
import com.redhat.service.smartevents.shard.operator.v1.resources.knative.KnativeBroker;
import com.redhat.service.smartevents.shard.operator.v1.utils.KubernetesResourcePatcher;
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
        assertThat(updateControl.isUpdateStatus()).isTrue();
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
        assertThat(bridgeIngress.getStatus().getConditionByType(ConditionTypeConstants.READY)).isPresent().hasValueSatisfying(c -> {
            assertThat(c.getStatus()).isEqualTo(ConditionStatus.False);
        });
        assertThat(bridgeIngress.getStatus().getConditionByType(BridgeIngressStatus.SECRET_AVAILABLE)).isPresent().hasValueSatisfying(c -> {
            assertThat(c.getStatus()).isEqualTo(ConditionStatus.True);
        });
        assertThat(bridgeIngress.getStatus().getConditionByType(BridgeIngressStatus.CONFIG_MAP_AVAILABLE)).isPresent().hasValueSatisfying(c -> {
            assertThat(c.getStatus()).isEqualTo(ConditionStatus.True);
        });
        assertThat(bridgeIngress.getStatus().getConditionByType(BridgeIngressStatus.KNATIVE_BROKER_AVAILABLE)).isPresent().hasValueSatisfying(c -> {
            assertThat(c.getStatus()).isEqualTo(ConditionStatus.False);
        });
        assertThat(bridgeIngress.getStatus().getConditionByType(BridgeIngressStatus.AUTHORISATION_POLICY_AVAILABLE)).isPresent().hasValueSatisfying(c -> {
            assertThat(c.getStatus()).isEqualTo(ConditionStatus.Unknown);
        });
        assertThat(bridgeIngress.getStatus().getConditionByType(BridgeIngressStatus.NETWORK_RESOURCE_AVAILABLE)).isPresent().hasValueSatisfying(c -> {
            assertThat(c.getStatus()).isEqualTo(ConditionStatus.Unknown);
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
                .withHost(TestSupport.BRIDGE_HOST)
                .build();
    }
}
