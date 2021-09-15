package com.redhat.service.bridge.shard.controllers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.redhat.service.bridge.executor.ExecutorsService;
import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.dto.BridgeDTO;
import com.redhat.service.bridge.infra.dto.BridgeStatus;
import com.redhat.service.bridge.infra.dto.ProcessorDTO;
import com.redhat.service.bridge.shard.AbstractShardWireMockTest;
import com.redhat.service.bridge.shard.OperatorService;

import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.mockito.Mockito.verify;

@QuarkusTest
public class ProcessorControllerTest extends AbstractShardWireMockTest {

    ExecutorsService executorsService;

    @Inject
    OperatorService operatorService;

    @BeforeEach
    public void beforeEach() {
        super.beforeEach();
        executorsService = Mockito.mock(ExecutorsService.class);
        QuarkusMock.installMockForType(executorsService, ExecutorsService.class);
    }

    @Test
    public void reconcileProcessor() throws Exception {
        BridgeDTO bridge = new BridgeDTO("myId-1", "myName-1", "myEndpoint", "myCustomerId", BridgeStatus.AVAILABLE);
        ProcessorDTO processor = new ProcessorDTO("processorId-1", "processorName-1", bridge, BridgeStatus.PROVISIONING);

        stubProcessorUpdate();

        CountDownLatch latch = new CountDownLatch(1);
        addProcessorUpdateRequestListener(latch);

        operatorService.createProcessorDeployment(processor);

        Assertions.assertTrue(latch.await(30, TimeUnit.SECONDS));

        processor.setStatus(BridgeStatus.AVAILABLE);

        WireMock.verify(putRequestedFor(urlEqualTo(APIConstants.SHARD_API_BASE_PATH + "processors"))
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(processor), true, true))
                .withHeader("Content-Type", equalTo("application/json")));

        verify(executorsService).createExecutor(processor);
    }

    @Test
    public void reconcileProcessor_withFailure() throws Exception {

        BridgeDTO bridge = new BridgeDTO("myId-1", "myName-1", "myEndpoint", "myCustomerId", BridgeStatus.AVAILABLE);
        ProcessorDTO processor = new ProcessorDTO("processorId-1", "processorName-1", bridge, BridgeStatus.PROVISIONING);

        stubProcessorUpdate();

        CountDownLatch latch = new CountDownLatch(1);
        addProcessorUpdateRequestListener(latch);

        Mockito.doThrow(new RuntimeException("Test Mock Failure: This is an expected Failure for Testing only.")).when(executorsService).createExecutor(processor);

        operatorService.createProcessorDeployment(processor);

        Assertions.assertTrue(latch.await(30, TimeUnit.SECONDS));

        processor.setStatus(BridgeStatus.FAILED);

        WireMock.verify(putRequestedFor(urlEqualTo(APIConstants.SHARD_API_BASE_PATH + "processors"))
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(processor), true, true))
                .withHeader("Content-Type", equalTo("application/json")));

        verify(executorsService).createExecutor(processor);
    }
}
