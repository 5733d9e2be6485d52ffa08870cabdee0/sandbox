package com.redhat.service.bridge.shard.operator;

import java.time.Duration;

import javax.inject.Inject;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@QuarkusTest
@WithKubernetesTestServer
public class BridgeIngressServiceTest {

    @Inject
    BridgeIngressService bridgeIngressService;

    @Inject
    KubernetesClient kubernetesClient;

    @Test
    public void testBridgeIngressCreation() {
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
                            Deployment deployment = kubernetesClient.apps().deployments()
                                    .inNamespace(KubernetesResourceUtil.sanitizeName(TestConstants.CUSTOMER_ID))
                                    .withName(KubernetesResourceUtil.sanitizeName(TestConstants.BRIDGE_ID))
                                    .get();
                            assertThat(deployment).isNotNull();
                        });
    }

    @Test
    @Disabled("Bug in the sdk? It's working on real k8s clusters")
    public void testBridgeIngressDeletion() {
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
                                    .inNamespace(KubernetesResourceUtil.sanitizeName(TestConstants.CUSTOMER_ID))
                                    .withName(KubernetesResourceUtil.sanitizeName(TestConstants.BRIDGE_ID))
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
                                    .inNamespace(KubernetesResourceUtil.sanitizeName(TestConstants.CUSTOMER_ID))
                                    .withName(KubernetesResourceUtil.sanitizeName(TestConstants.BRIDGE_ID))
                                    .get();
                            assertThat(deployment).isNull();
                        });
    }
}
