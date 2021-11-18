package com.redhat.service.bridge.shard.operator;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.shard.operator.providers.CustomerNamespaceProvider;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngress;
import com.redhat.service.bridge.shard.operator.utils.KubernetesResourcePatcher;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatusBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@WithOpenShiftTestServer
public class ManagerSyncServiceTest extends AbstractShardWireMockTest {

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    CustomerNamespaceProvider customerNamespaceProvider;

    @Inject
    KubernetesResourcePatcher kubernetesResourcePatcher;

    @BeforeEach
    public void setup() {
        // Kubernetes Server must be cleaned up at startup of every test.
        kubernetesClient.resources(BridgeIngress.class).inAnyNamespace().delete();
    }

    @Test
    public void testBridgesAreDeployed() throws JsonProcessingException, InterruptedException {
        List<BridgeDTO> bridgeDTOS = new ArrayList<>();
        bridgeDTOS.add(new BridgeDTO("myId-1", "myName-1", "myEndpoint", TestConstants.CUSTOMER_ID, BridgeStatus.REQUESTED));
        bridgeDTOS.add(new BridgeDTO("myId-2", "myName-2", "myEndpoint", TestConstants.CUSTOMER_ID, BridgeStatus.REQUESTED));
        stubBridgesToDeployOrDelete(bridgeDTOS);
        stubBridgeUpdate();
        String expectedJsonUpdateProvisioningRequest =
                String.format("{\"id\": \"myId-1\", \"name\": \"myName-1\", \"endpoint\": \"myEndpoint\", \"customerId\": \"%s\", \"status\": \"PROVISIONING\"}", TestConstants.CUSTOMER_ID);
        String expectedJsonUpdateAvailableRequest =
                String.format("{\"id\": \"myId-1\", \"name\": \"myName-1\", \"endpoint\": \"http://192.168.2.49/myId-1\", \"customerId\": \"%s\", \"status\": \"AVAILABLE\"}",
                        TestConstants.CUSTOMER_ID);

        CountDownLatch latch = new CountDownLatch(4); // Four updates to the manager are expected (2 PROVISIONING + 2 AVAILABLE)
        addBridgeUpdateRequestListener(latch);

        managerSyncService.fetchAndProcessBridgesToDeployOrDelete().await().atMost(Duration.ofSeconds(5));

        String customerNamespace = customerNamespaceProvider.resolveName(TestConstants.CUSTOMER_ID);
        String firstBridgeName = KubernetesResourceUtil.sanitizeName("myId-1");
        String secondBridgeName = KubernetesResourceUtil.sanitizeName("myId-2");
        Awaitility.await()
                .atMost(Duration.ofMinutes(2))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> {
                            kubernetesResourcePatcher.patchReadyDeploymentOrFail(firstBridgeName, customerNamespace);
                            kubernetesResourcePatcher.patchReadyDeploymentOrFail(secondBridgeName, customerNamespace);
                            kubernetesResourcePatcher.patchReadyServiceOrFail(firstBridgeName, customerNamespace);
                            kubernetesResourcePatcher.patchReadyServiceOrFail(secondBridgeName, customerNamespace);
                            kubernetesResourcePatcher.patchReadyNetworkResourceOrFail(firstBridgeName, customerNamespace);
                            kubernetesResourcePatcher.patchReadyNetworkResourceOrFail(secondBridgeName, customerNamespace);
                        });

        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
        wireMockServer.verify(putRequestedFor(urlEqualTo(APIConstants.SHARD_API_BASE_PATH))
                .withRequestBody(equalToJson(expectedJsonUpdateProvisioningRequest, true, true))
                .withHeader("Content-Type", equalTo("application/json")));
        wireMockServer.verify(putRequestedFor(urlEqualTo(APIConstants.SHARD_API_BASE_PATH))
                .withRequestBody(equalToJson(expectedJsonUpdateAvailableRequest, true, true))
                .withHeader("Content-Type", equalTo("application/json")));
    }

    @Test
    public void testBridgesAreDeleted() throws JsonProcessingException, InterruptedException {
        List<BridgeDTO> bridgeDTOS = new ArrayList<>();
        bridgeDTOS.add(new BridgeDTO("myId-1", "myName-1", "myEndpoint", "myCustomerId", BridgeStatus.DELETION_REQUESTED));
        bridgeDTOS.add(new BridgeDTO("myId-2", "myName-2", "myEndpoint", "myCustomerId", BridgeStatus.DELETION_REQUESTED));
        stubBridgesToDeployOrDelete(bridgeDTOS);
        stubBridgeUpdate();
        String expectedJsonUpdateRequest = "{\"id\": \"myId-1\", \"name\": \"myName-1\", \"endpoint\": \"myEndpoint\", \"customerId\": \"myCustomerId\", \"status\": \"DELETED\"}";

        CountDownLatch latch = new CountDownLatch(2); // Two updates to the manager are expected
        addBridgeUpdateRequestListener(latch);

        managerSyncService.fetchAndProcessBridgesToDeployOrDelete().await().atMost(Duration.ofSeconds(5));

        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
        wireMockServer.verify(putRequestedFor(urlEqualTo(APIConstants.SHARD_API_BASE_PATH))
                .withRequestBody(equalToJson(expectedJsonUpdateRequest, true, true))
                .withHeader("Content-Type", equalTo("application/json")));
    }

    @Test
    public void testNotifyBridgeStatusChange() throws InterruptedException {
        BridgeDTO dto = new BridgeDTO("myId-1", "myName-1", "myEndpoint", "myCustomerId", BridgeStatus.PROVISIONING);
        stubBridgeUpdate();
        String expectedJsonUpdate = "{\"id\": \"myId-1\", \"name\": \"myName-1\", \"endpoint\": \"myEndpoint\", \"customerId\": \"myCustomerId\", \"status\": \"PROVISIONING\"}";

        CountDownLatch latch = new CountDownLatch(1); // One update to the manager is expected
        addBridgeUpdateRequestListener(latch);

        managerSyncService.notifyBridgeStatusChange(dto).await().atMost(Duration.ofSeconds(5));

        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
        wireMockServer.verify(putRequestedFor(urlEqualTo(APIConstants.SHARD_API_BASE_PATH))
                .withRequestBody(equalToJson(expectedJsonUpdate, true, true))
                .withHeader("Content-Type", equalTo("application/json")));
    }

    @Test
    public void testNotifyBridgeStatusChange1() throws InterruptedException {
        BridgeDTO dto = new BridgeDTO("myId-1", "myName-1", "myEndpoint", "myCustomerId", BridgeStatus.PROVISIONING);
        stubBridgeUpdate();
        String expectedJsonUpdate = "{\"id\": \"myId-1\", \"name\": \"myName-1\", \"endpoint\": \"myEndpoint\", \"customerId\": \"myCustomerId\", \"status\": \"PROVISIONING\"}";

        CountDownLatch latch = new CountDownLatch(1); // One update to the manager is expected
        addBridgeUpdateRequestListener(latch);

        managerSyncService.notifyBridgeStatusChange(dto).await().atMost(Duration.ofSeconds(5));

        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
        wireMockServer.verify(putRequestedFor(urlEqualTo(APIConstants.SHARD_API_BASE_PATH))
                .withRequestBody(equalToJson(expectedJsonUpdate, true, true))
                .withHeader("Content-Type", equalTo("application/json")));
    }

    @Test
    @Disabled("Operator does not support processors yet")
    public void testProcessorsAreDeployed() throws Exception {
        BridgeDTO bridge = new BridgeDTO("myId-1", "myName-1", "myEndpoint", "myCustomerId", BridgeStatus.AVAILABLE);
        ProcessorDTO processor = createProcessor(bridge, BridgeStatus.REQUESTED);

        stubProcessorsToDeployOrDelete(Collections.singletonList(processor));
        stubProcessorUpdate();

        CountDownLatch latch = new CountDownLatch(1);
        addProcessorUpdateRequestListener(latch);
        managerSyncService.fetchAndProcessProcessorsToDeployOrDelete().await().atMost(Duration.ofSeconds(5));
        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();

        processor.setStatus(BridgeStatus.PROVISIONING);

        wireMockServer.verify(putRequestedFor(urlEqualTo(APIConstants.SHARD_API_BASE_PATH + "processors"))
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(processor), true, true))
                .withHeader("Content-Type", equalTo("application/json")));
    }

    @Test
    @Disabled("Operator does not support processors yet")
    public void notifyProcessorStatusChange() throws Exception {
        BridgeDTO dto = new BridgeDTO("myId-1", "myName-1", "myEndpoint", "myCustomerId", BridgeStatus.AVAILABLE);
        ProcessorDTO processor = createProcessor(dto, BridgeStatus.PROVISIONING);
        stubProcessorUpdate();

        CountDownLatch latch = new CountDownLatch(1); // One update to the manager is expected
        addProcessorUpdateRequestListener(latch);

        managerSyncService.notifyProcessorStatusChange(processor).await().atMost(Duration.ofSeconds(5));

        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
        wireMockServer.verify(putRequestedFor(urlEqualTo(APIConstants.SHARD_API_BASE_PATH + "processors"))
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(processor), true, true))
                .withHeader("Content-Type", equalTo("application/json")));
    }

    private void patchDeployment(String name, String namespace) {
        Deployment deployment = kubernetesClient.apps().deployments()
                .inNamespace(namespace)
                .withName(name)
                .get();
        assertThat(deployment).isNotNull();
        deployment.setStatus(new DeploymentStatusBuilder().withAvailableReplicas(1).withReplicas(1).build());
        kubernetesClient.apps().deployments()
                .inNamespace(namespace)
                .withName(name)
                .replace(deployment);
    }
}
