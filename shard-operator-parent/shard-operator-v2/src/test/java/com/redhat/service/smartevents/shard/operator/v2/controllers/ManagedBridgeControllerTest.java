package com.redhat.service.smartevents.shard.operator.v2.controllers;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.shard.operator.core.providers.GlobalConfigurationsConstants;
import com.redhat.service.smartevents.shard.operator.core.resources.ConditionStatus;
import com.redhat.service.smartevents.shard.operator.core.resources.knative.KnativeBroker;
import com.redhat.service.smartevents.shard.operator.v2.providers.NamespaceProvider;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridge;
import com.redhat.service.smartevents.shard.operator.v2.utils.Fixtures;
import com.redhat.service.smartevents.shard.operator.v2.utils.V2KubernetesResourcePatcher;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;

import static com.redhat.service.smartevents.infra.v2.api.models.DefaultConditions.DP_AUTHORISATION_POLICY_READY_NAME;
import static com.redhat.service.smartevents.infra.v2.api.models.DefaultConditions.DP_CONFIG_MAP_READY_NAME;
import static com.redhat.service.smartevents.infra.v2.api.models.DefaultConditions.DP_KNATIVE_BROKER_READY_NAME;
import static com.redhat.service.smartevents.infra.v2.api.models.DefaultConditions.DP_NETWORK_RESOURCE_READY_NAME;
import static com.redhat.service.smartevents.infra.v2.api.models.DefaultConditions.DP_SECRET_READY_NAME;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@WithOpenShiftTestServer
public class ManagedBridgeControllerTest {

    @Inject
    ManagedBridgeController managedBridgeController;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    NamespaceProvider namespaceProvider;

    @Inject
    V2KubernetesResourcePatcher kubernetesResourcePatcher;

    @BeforeEach
    public void beforeEach() {
        kubernetesResourcePatcher.cleanUp();
    }

    private ManagedBridge createManagedBridge() {
        BridgeDTO bridgeDTO = Fixtures.createBridge(OperationType.CREATE);
        return Fixtures.createManagedBridge(bridgeDTO, namespaceProvider.getNamespaceName(bridgeDTO.getId()));
    }

    @Test
    public void createManagedBridgeWithoutSecrets() {
        ManagedBridge managedBridge = createManagedBridge();
        UpdateControl<ManagedBridge> control = managedBridgeController.reconcile(managedBridge, null);

        assertThat(control.isUpdateStatus()).isTrue();
        assertThat(managedBridge.getStatus().isConditionTypeFalse(DP_SECRET_READY_NAME)).isTrue();
    }

    private void deployManagedBridgeSecret(ManagedBridge managedBridge) {
        Map<String, String> data = new HashMap<>();
        data.put(GlobalConfigurationsConstants.KNATIVE_KAFKA_BOOTSTRAP_SERVERS_SECRET, Base64.getEncoder().encodeToString("bootstrap.servers".getBytes(StandardCharsets.UTF_8)));
        data.put(GlobalConfigurationsConstants.KNATIVE_KAFKA_TOPIC_NAME_SECRET, Base64.getEncoder().encodeToString("topic".getBytes(StandardCharsets.UTF_8)));

        Secret secret = new SecretBuilder()
                .withMetadata(
                        new ObjectMetaBuilder()
                                .withNamespace(managedBridge.getMetadata().getNamespace())
                                .withName(managedBridge.getMetadata().getName())
                                .build())
                .withData(data)
                .build();
        kubernetesClient
                .secrets()
                .inNamespace(managedBridge.getMetadata().getNamespace())
                .withName(managedBridge.getMetadata().getName())
                .createOrReplace(secret);
    }

    @Test
    void testCreateNewManagedBridge() {
        // Given
        ManagedBridge managedBridge = createManagedBridge();
        deployManagedBridgeSecret(managedBridge);

        // When
        UpdateControl<ManagedBridge> updateControl = managedBridgeController.reconcile(managedBridge, null);

        // Then
        assertThat(updateControl.isUpdateStatus()).isTrue();
        assertThat(managedBridge.getStatus()).isNotNull();
        assertThat(managedBridge.getStatus().getConditionByType(DP_SECRET_READY_NAME)).isPresent().hasValueSatisfying(c -> {
            assertThat(c.getStatus()).isEqualTo(ConditionStatus.True);
        });
        assertThat(managedBridge.getStatus().getConditionByType(DP_CONFIG_MAP_READY_NAME)).isPresent().hasValueSatisfying(c -> {
            assertThat(c.getStatus()).isEqualTo(ConditionStatus.True);
        });
        assertThat(managedBridge.getStatus().getConditionByType(DP_KNATIVE_BROKER_READY_NAME)).isPresent().hasValueSatisfying(c -> {
            assertThat(c.getStatus()).isEqualTo(ConditionStatus.False);
        });
        assertThat(managedBridge.getStatus().getConditionByType(DP_AUTHORISATION_POLICY_READY_NAME)).isPresent().hasValueSatisfying(c -> {
            assertThat(c.getStatus()).isEqualTo(ConditionStatus.Unknown);
        });
        assertThat(managedBridge.getStatus().getConditionByType(DP_NETWORK_RESOURCE_READY_NAME)).isPresent().hasValueSatisfying(c -> {
            assertThat(c.getStatus()).isEqualTo(ConditionStatus.Unknown);
        });
    }

    @Test
    void testBridgeIngressKnativeBroker() {
        // Given
        ManagedBridge managedBridge = createManagedBridge();
        deployManagedBridgeSecret(managedBridge);

        // When
        UpdateControl<ManagedBridge> updateControl = managedBridgeController.reconcile(managedBridge, null);

        // Then
        KnativeBroker knativeBroker = kubernetesClient
                .resources(KnativeBroker.class)
                .inNamespace(managedBridge.getMetadata().getNamespace())
                .withName(managedBridge.getMetadata().getName())
                .get();

        assertThat(knativeBroker).isNotNull();
        assertThat(knativeBroker.getMetadata().getOwnerReferences().size()).isEqualTo(1);
        assertThat(knativeBroker.getMetadata().getLabels()).isNotNull();
        assertThat(knativeBroker.getSpec().getConfig().getApiVersion()).isNotNull();
        assertThat(knativeBroker.getSpec().getConfig().getName()).isNotNull();
        assertThat(knativeBroker.getSpec().getConfig().getNamespace()).isNotNull();
        assertThat(knativeBroker.getSpec().getConfig().getKind()).isNotNull();

    }

}
