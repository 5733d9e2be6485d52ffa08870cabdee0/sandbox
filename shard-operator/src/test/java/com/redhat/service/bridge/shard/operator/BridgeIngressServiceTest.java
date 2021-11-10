package com.redhat.service.bridge.shard.operator;

import java.time.Duration;

import javax.inject.Inject;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.shard.operator.providers.CustomerNamespaceProvider;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngress;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@QuarkusTest
@WithOpenShiftTestServer
public class BridgeIngressServiceTest {

    @Inject
    BridgeIngressService bridgeIngressService;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    CustomerNamespaceProvider customerNamespaceProvider;

    @BeforeEach
    public void setup() {
        // Kubernetes Server must be cleaned up at startup of every test.
        kubernetesClient.resources(BridgeIngress.class).inAnyNamespace().delete();
    }

    @Test
    public void testBridgeIngressCreation() {
        // Given
        BridgeDTO dto = new BridgeDTO(TestConstants.BRIDGE_ID, TestConstants.BRIDGE_NAME, "myEndpoint", TestConstants.CUSTOMER_ID, BridgeStatus.PROVISIONING);

        // When
        bridgeIngressService.createBridgeIngress(dto);

        // Then
        BridgeIngress bridgeIngress = kubernetesClient
                .resources(BridgeIngress.class)
                .inNamespace(customerNamespaceProvider.resolveName(dto.getCustomerId()))
                .withName(KubernetesResourceUtil.sanitizeName(dto.getId()))
                .get();
        assertThat(bridgeIngress).isNotNull();
    }

    @Test
    public void testBridgeIngressCreationTriggersController() {
        // Given
        BridgeDTO dto = new BridgeDTO(TestConstants.BRIDGE_ID, TestConstants.BRIDGE_NAME, "myEndpoint", TestConstants.CUSTOMER_ID, BridgeStatus.PROVISIONING);

        // When
        bridgeIngressService.createBridgeIngress(dto);

        // Then
        Awaitility.await()
                .atMost(Duration.ofMinutes(2))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> {
                            // The deployment is deployed by the controller
                            Deployment deployment = kubernetesClient.apps().deployments()
                                    .inNamespace(customerNamespaceProvider.resolveName(dto.getCustomerId()))
                                    .withName(KubernetesResourceUtil.sanitizeName(dto.getId()))
                                    .get();
                            assertThat(deployment).isNotNull();
                        });
    }

    @Test
    public void testBridgeIngressDeletion() {
        // Given
        BridgeDTO dto = new BridgeDTO(TestConstants.BRIDGE_ID, TestConstants.BRIDGE_NAME, "myEndpoint", TestConstants.CUSTOMER_ID, BridgeStatus.PROVISIONING);

        // When
        bridgeIngressService.createBridgeIngress(dto);
        bridgeIngressService.deleteBridgeIngress(dto);

        // Then
        BridgeIngress bridgeIngress = kubernetesClient
                .resources(BridgeIngress.class)
                .inNamespace(customerNamespaceProvider.resolveName(dto.getCustomerId()))
                .withName(KubernetesResourceUtil.sanitizeName(dto.getId()))
                .get();
        assertThat(bridgeIngress).isNull();
    }

    @Test
    @Disabled("Delete loop in BridgeIngressController does not get called. Bug in the SDK? https://issues.redhat.com/browse/MGDOBR-128")
    public void testBridgeIngressDeletionRemovesAllLinkedResource() {
        // Given
        BridgeDTO dto = new BridgeDTO(TestConstants.BRIDGE_ID, TestConstants.BRIDGE_NAME, "myEndpoint", TestConstants.CUSTOMER_ID, BridgeStatus.PROVISIONING);

        // When
        bridgeIngressService.createBridgeIngress(dto);
        Awaitility.await()
                .atMost(Duration.ofMinutes(2))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> {
                            Deployment deployment = kubernetesClient.apps().deployments()
                                    .inNamespace(customerNamespaceProvider.resolveName(dto.getCustomerId()))
                                    .withName(KubernetesResourceUtil.sanitizeName(dto.getId()))
                                    .get();
                            assertThat(deployment).isNotNull();
                        });
        bridgeIngressService.deleteBridgeIngress(dto);

        // Then
        Awaitility.await()
                .atMost(Duration.ofMinutes(2))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> {
                            Deployment deployment = kubernetesClient.apps().deployments()
                                    .inNamespace(customerNamespaceProvider.resolveName(dto.getCustomerId()))
                                    .withName(KubernetesResourceUtil.sanitizeName(dto.getId()))
                                    .get();
                            assertThat(deployment).isNull();
                        });
    }
}
