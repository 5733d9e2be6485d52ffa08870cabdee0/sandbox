package com.redhat.service.bridge.shard;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;

import io.quarkus.test.junit.QuarkusTest;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class ManagerSyncServiceTest extends AbstractShardWireMockTest {

    @Test
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
}
