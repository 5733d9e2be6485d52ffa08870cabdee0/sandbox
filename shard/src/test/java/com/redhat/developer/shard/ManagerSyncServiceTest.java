package com.redhat.developer.shard;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.redhat.developer.infra.api.APIConstants;
import com.redhat.developer.infra.dto.BridgeDTO;
import com.redhat.developer.infra.dto.BridgeStatus;
import com.redhat.developer.infra.dto.ProcessorDTO;

import io.quarkus.test.junit.QuarkusTest;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.in;

@QuarkusTest
public class ManagerSyncServiceTest extends AbstractShardWireMockTest {

    @Test
    public void testBridgesAreDeployed() throws JsonProcessingException, InterruptedException {
        List<BridgeDTO> bridgeDTOS = new ArrayList<>();
        bridgeDTOS.add(new BridgeDTO("myId-1", "myName-1", "myEndpoint", "myCustomerId", BridgeStatus.REQUESTED));
        bridgeDTOS.add(new BridgeDTO("myId-2", "myName-2", "myEndpoint", "myCustomerId", BridgeStatus.REQUESTED));
        stubBridgesToDeployOrDelete(bridgeDTOS);
        stubBridgeUpdate();
        String expectedJsonUpdateRequest = "{\"id\": \"myId-1\", \"name\": \"myName-1\", \"endpoint\": \"myEndpoint\", \"customerId\": \"myCustomerId\", \"status\": \"PROVISIONING\"}";

        CountDownLatch latch = new CountDownLatch(2); // Two updates to the manager are expected
        addBridgeUpdateRequestListener(latch);

        managerSyncService.fetchAndProcessBridgesToDeployOrDelete().await().atMost(Duration.ofSeconds(5));

        Assertions.assertTrue(latch.await(30, TimeUnit.SECONDS));
        verify(putRequestedFor(urlEqualTo(APIConstants.SHARD_API_BASE_PATH))
                .withRequestBody(equalToJson(expectedJsonUpdateRequest, true, true))
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

        Assertions.assertTrue(latch.await(30, TimeUnit.SECONDS));
        verify(putRequestedFor(urlEqualTo(APIConstants.SHARD_API_BASE_PATH))
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

        Assertions.assertTrue(latch.await(30, TimeUnit.SECONDS));
        verify(putRequestedFor(urlEqualTo(APIConstants.SHARD_API_BASE_PATH))
                .withRequestBody(equalToJson(expectedJsonUpdate, true, true))
                .withHeader("Content-Type", equalTo("application/json")));
    }

    @Test
    public void fetchProcessorsForBridge() throws Exception {
        BridgeDTO bridge = new BridgeDTO("myId-1", "myName-1", "myEndpoint", "myCustomerId", BridgeStatus.AVAILABLE);
        ProcessorDTO processor = new ProcessorDTO("processorId-1", "processorName-1", bridge, BridgeStatus.PROVISIONING);
        ProcessorDTO processor2 = new ProcessorDTO("processorId-2", "processorName-2", bridge, BridgeStatus.DELETION_REQUESTED);

        stubProcessorsToDeployOrDelete(bridge, asList(processor, processor2));

        Stream<ProcessorDTO> fetchedProcessors = managerSyncService.fetchProcessorsForBridge(bridge).subscribe().asStream();
        fetchedProcessors.forEach((p) -> assertThat(p.getId(), in(asList("processorId-1", "processorId-2"))));
    }

    @Test
    public void notifyProcessorStatusChange() throws Exception {
        BridgeDTO dto = new BridgeDTO("myId-1", "myName-1", "myEndpoint", "myCustomerId", BridgeStatus.AVAILABLE);
        ProcessorDTO processor = new ProcessorDTO("processorId-1", "processorName-1", dto, BridgeStatus.PROVISIONING);
        stubProcessorUpdate(dto);

        CountDownLatch latch = new CountDownLatch(1); // One update to the manager is expected
        addProcessorUpdateRequestListener(dto, latch);

        managerSyncService.notifyProcessorStatusChange(processor).await().atMost(Duration.ofSeconds(5));

        Assertions.assertTrue(latch.await(30, TimeUnit.SECONDS));
        verify(putRequestedFor(urlEqualTo(APIConstants.SHARD_API_BASE_PATH + dto.getId() + "/processors"))
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(processor), true, true))
                .withHeader("Content-Type", equalTo("application/json")));
    }
}
