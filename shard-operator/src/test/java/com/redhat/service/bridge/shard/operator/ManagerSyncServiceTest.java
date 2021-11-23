package com.redhat.service.bridge.shard.operator;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.shard.operator.providers.CustomerNamespaceProvider;
import com.redhat.service.bridge.shard.operator.resources.BridgeExecutor;
import com.redhat.service.bridge.shard.operator.resources.BridgeIngress;

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
    CustomerNamespaceProvider customerNamespaceProvider;

    @Test
    public void testBridgesAreDeployed() throws JsonProcessingException, InterruptedException {
        List<BridgeDTO> bridgeDTOS = new ArrayList<>();
        bridgeDTOS.add(new BridgeDTO("myId-1", "myName-1", "myEndpoint", TestSupport.CUSTOMER_ID, BridgeStatus.REQUESTED));
        bridgeDTOS.add(new BridgeDTO("myId-2", "myName-2", "myEndpoint", TestSupport.CUSTOMER_ID, BridgeStatus.REQUESTED));
        stubBridgesToDeployOrDelete(bridgeDTOS);
        stubBridgeUpdate();
        String expectedJsonUpdateProvisioningRequest =
                String.format("{\"id\": \"myId-1\", \"name\": \"myName-1\", \"endpoint\": \"myEndpoint\", \"customerId\": \"%s\", \"status\": \"PROVISIONING\"}", TestSupport.CUSTOMER_ID);
        String expectedJsonUpdateAvailableRequest =
                String.format("{\"id\": \"myId-1\", \"name\": \"myName-1\", \"endpoint\": \"http://192.168.2.49/ob-myId-1\", \"customerId\": \"%s\", \"status\": \"AVAILABLE\"}",
                        TestSupport.CUSTOMER_ID);

        CountDownLatch latch = new CountDownLatch(4); // Four updates to the manager are expected (2 PROVISIONING + 2 AVAILABLE)
        addBridgeUpdateRequestListener(latch);

        managerSyncService.fetchAndProcessBridgesToDeployOrDelete().await().atMost(Duration.ofSeconds(5));

        String customerNamespace = customerNamespaceProvider.resolveName(TestSupport.CUSTOMER_ID);
        String firstBridgeName = BridgeIngress.resolveResourceName("myId-1");
        String secondBridgeName = BridgeIngress.resolveResourceName("myId-2");
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
    @Disabled
    public void testProcessorsAreDeployed() throws Exception {
        ProcessorDTO processor = TestSupport.newRequestedProcessorDTO();

        stubProcessorsToDeployOrDelete(Collections.singletonList(processor));
        stubProcessorUpdate();

        CountDownLatch latch = new CountDownLatch(2); // Two updates to the manager are expected (1 PROVISIONING + 1 AVAILABLE)
        addProcessorUpdateRequestListener(latch);
        managerSyncService.fetchAndProcessProcessorsToDeployOrDelete().await().atMost(Duration.ofSeconds(5));

        String customerNamespace = customerNamespaceProvider.resolveName(TestSupport.CUSTOMER_ID);
        String sanitizedName = BridgeExecutor.resolveResourceName(processor.getId());
        Awaitility.await()
                .atMost(Duration.ofMinutes(2))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> {
                            kubernetesResourcePatcher.patchReadyDeploymentOrFail(sanitizedName, customerNamespace);
                            kubernetesResourcePatcher.patchReadyServiceOrFail(sanitizedName, customerNamespace);
                        });

        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();

        processor.setStatus(BridgeStatus.AVAILABLE);
        wireMockServer.verify(putRequestedFor(urlEqualTo(APIConstants.SHARD_API_BASE_PATH + "processors"))
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(processor), true, true))
                .withHeader("Content-Type", equalTo("application/json")));
    }

    @Test
    @Disabled
    public void notifyProcessorStatusChange() throws Exception {
        ProcessorDTO processor = TestSupport.newRequestedProcessorDTO();
        stubProcessorUpdate();

        CountDownLatch latch = new CountDownLatch(1); // One update to the manager is expected
        addProcessorUpdateRequestListener(latch);

        managerSyncService.notifyProcessorStatusChange(processor).await().atMost(Duration.ofSeconds(5));

        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
        wireMockServer.verify(putRequestedFor(urlEqualTo(APIConstants.SHARD_API_BASE_PATH + "processors"))
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(processor), true, true))
                .withHeader("Content-Type", equalTo("application/json")));
    }
}
