package com.redhat.developer.shard.controllers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.redhat.developer.executor.ExecutorsService;
import com.redhat.developer.infra.api.APIConstants;
import com.redhat.developer.infra.dto.BridgeDTO;
import com.redhat.developer.infra.dto.BridgeStatus;
import com.redhat.developer.infra.dto.ProcessorDTO;
import com.redhat.developer.shard.AbstractShardWireMockTest;

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
    ProcessorController processorController;

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

        processorController.deployProcessor(processor);
        processorController.reconcileProcessors();

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

        Mockito.doThrow(new RuntimeException("Failed to provision executor")).when(executorsService).createExecutor(processor);

        processorController.deployProcessor(processor);
        processorController.reconcileProcessors();

        Assertions.assertTrue(latch.await(30, TimeUnit.SECONDS));

        processor.setStatus(BridgeStatus.FAILED);

        WireMock.verify(putRequestedFor(urlEqualTo(APIConstants.SHARD_API_BASE_PATH + "processors"))
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(processor), true, true))
                .withHeader("Content-Type", equalTo("application/json")));

        verify(executorsService).createExecutor(processor);
    }
}
