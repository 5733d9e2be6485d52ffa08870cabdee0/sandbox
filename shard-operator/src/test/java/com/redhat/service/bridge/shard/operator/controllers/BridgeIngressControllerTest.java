package com.redhat.service.bridge.shard.operator.controllers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.shard.operator.AbstractShardWireMockTest;
import com.redhat.service.bridge.shard.operator.TestSupport;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngress;
import com.redhat.service.bridge.shard.operator.resources.ConditionReason;
import com.redhat.service.bridge.shard.operator.resources.ConditionStatus;
import com.redhat.service.bridge.shard.operator.resources.ConditionType;
import com.redhat.service.bridge.shard.operator.utils.KubernetesResourcePatcher;
import com.redhat.service.bridge.test.resource.KeycloakResource;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@WithOpenShiftTestServer
@QuarkusTestResource(KeycloakResource.class)
public class BridgeIngressControllerTest extends AbstractShardWireMockTest {

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
        UpdateControl<BridgeIngress> updateControl = bridgeIngressController.createOrUpdateResource(bridgeIngress, null);

        // Then
        assertThat(updateControl.isUpdateStatusSubResource()).isTrue();
        assertThat(bridgeIngress.getStatus()).isNotNull();
        assertThat(bridgeIngress.getStatus().isReady()).isFalse();
        assertThat(bridgeIngress.getStatus().getConditionByType(ConditionType.Augmentation)).isPresent().hasValueSatisfying(c -> {
            assertThat(c.getStatus()).isEqualTo(ConditionStatus.False);
        });
        assertThat(bridgeIngress.getStatus().getConditionByType(ConditionType.Ready)).isPresent().hasValueSatisfying(c -> {
            assertThat(c.getStatus()).isEqualTo(ConditionStatus.False);
            assertThat(c.getReason()).isEqualTo(ConditionReason.SecretsNotFound);
        });
    }

    @Test
    void testCreateNewBridgeIngress() {
        // Given
        BridgeIngress bridgeIngress = buildBridgeIngress();
        deployBridgeIngressSecret(bridgeIngress);

        // When
        UpdateControl<BridgeIngress> updateControl = bridgeIngressController.createOrUpdateResource(bridgeIngress, null);

        // Then
        assertThat(updateControl.isUpdateStatusSubResource()).isTrue();
        assertThat(bridgeIngress.getStatus()).isNotNull();
        assertThat(bridgeIngress.getStatus().isReady()).isFalse();
        assertThat(bridgeIngress.getStatus().getConditionByType(ConditionType.Augmentation)).isPresent().hasValueSatisfying(c -> {
            assertThat(c.getStatus()).isEqualTo(ConditionStatus.False);
        });
        assertThat(bridgeIngress.getStatus().getConditionByType(ConditionType.Ready)).isPresent().hasValueSatisfying(c -> {
            assertThat(c.getStatus()).isEqualTo(ConditionStatus.False);
            assertThat(c.getReason()).isEqualTo(ConditionReason.DeploymentNotAvailable);
        });
    }

    @Test
    void testBridgeIngressDeployment() {
        // Given
        BridgeIngress bridgeIngress = buildBridgeIngress();
        deployBridgeIngressSecret(bridgeIngress);

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

    @Test
    void testBridgeIngressDeployment_deploymentFails() throws Exception {
        // Given
        BridgeIngress bridgeIngress = buildBridgeIngress();
        deployBridgeIngressSecret(bridgeIngress);

        // When
        bridgeIngressController.createOrUpdateResource(bridgeIngress, null);
        Deployment deployment = getDeploymentFor(bridgeIngress);

        // Then
        kubernetesResourcePatcher.patchDeploymentAsFailed(deployment.getMetadata().getName(), deployment.getMetadata().getNamespace());

        // We expect a single update to Fleet Manager to inform of the Deployment Failure
        stubBridgeUpdate();
        CountDownLatch bridgeUpdates = new CountDownLatch(1);
        addBridgeUpdateRequestListener(bridgeUpdates);

        UpdateControl<BridgeIngress> updateControl = bridgeIngressController.createOrUpdateResource(bridgeIngress, null);
        assertThat(updateControl.isUpdateStatusSubResource()).isTrue();

        BridgeDTO bridgeDTO = updateControl.getCustomResource().toDTO();
        bridgeDTO.setStatus(BridgeStatus.FAILED);

        assertThat(bridgeUpdates.await(30, TimeUnit.SECONDS)).isTrue();
        wireMockServer.verify(putRequestedFor(urlEqualTo(APIConstants.SHARD_API_BASE_PATH))
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(bridgeDTO)))
                .withHeader("Content-Type", equalTo("application/json")));
    }

    private void deployBridgeIngressSecret(BridgeIngress bridgeIngress) {
        Secret secret = new SecretBuilder()
                .withMetadata(new ObjectMetaBuilder().withNamespace(bridgeIngress.getMetadata().getNamespace()).withName(bridgeIngress.getMetadata().getName()).build())
                .build();
        kubernetesClient
                .secrets()
                .inNamespace(bridgeIngress.getMetadata().getNamespace())
                .withName(bridgeIngress.getMetadata().getName())
                .createOrReplace(secret);
    }

    private Deployment getDeploymentFor(BridgeIngress bridgeIngress) {
        Deployment deployment = kubernetesClient.apps().deployments().inNamespace(bridgeIngress.getMetadata().getNamespace()).withName(bridgeIngress.getMetadata().getName()).get();
        assertThat(deployment).isNotNull();
        return deployment;
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
