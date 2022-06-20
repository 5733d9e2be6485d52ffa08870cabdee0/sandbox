package com.redhat.service.smartevents.shard.operator;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.redhat.service.smartevents.infra.api.APIConstants;
import com.redhat.service.smartevents.infra.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.test.resource.KeycloakResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@QuarkusTest
@WithOpenShiftTestServer
@QuarkusTestResource(value = KeycloakResource.class, restrictToAnnotatedClass = true)
public class ManagerSyncServiceMockedTest extends AbstractManagerSyncServiceTest {

    @Inject
    ManagerSyncServiceImpl managerSyncService;

    @InjectMock
    BridgeIngressService bridgeIngressService;

    @InjectMock
    BridgeExecutorService bridgeExecutorService;

    @Test
    public void testBridgeDeployment() throws JsonProcessingException, InterruptedException {
        doThrow(new IllegalStateException()).when(bridgeIngressService).createBridgeIngress(any());

        BridgeDTO bridge = TestSupport.newRequestedBridgeDTO();
        stubBridgesToDeployOrDelete(List.of(bridge));
        stubBridgeUpdate();

        String expectedJsonUpdateProvisioningRequest =
                String.format("{\"id\": \"%s\", \"name\": \"%s\", \"endpoint\": \"%s\", \"customerId\": \"%s\", \"status\": \"provisioning\"}",
                        bridge.getId(),
                        bridge.getName(),
                        bridge.getEndpoint(),
                        bridge.getCustomerId());
        String expectedJsonUpdateFailedRequest =
                String.format("{\"id\": \"%s\", \"name\": \"%s\", \"endpoint\": \"%s\", \"customerId\": \"%s\", \"status\": \"failed\"}",
                        bridge.getId(),
                        bridge.getName(),
                        bridge.getEndpoint(),
                        bridge.getCustomerId());

        CountDownLatch latch = new CountDownLatch(2); // Two updates to the manager are expected (1 PROVISIONING + 1 FAILED)
        addBridgeUpdateRequestListener(latch);

        managerSyncService.doBridges().await().atMost(Duration.ofSeconds(5));

        assertThat(latch.await(60, TimeUnit.SECONDS)).isTrue();
        assertJsonRequest(expectedJsonUpdateProvisioningRequest, APIConstants.SHARD_API_BASE_PATH);
        assertJsonRequest(expectedJsonUpdateFailedRequest, APIConstants.SHARD_API_BASE_PATH);
    }

    @Test
    public void testBridgeDeletion() throws JsonProcessingException, InterruptedException {
        doThrow(new IllegalStateException()).when(bridgeIngressService).deleteBridgeIngress(any());

        BridgeDTO bridge = TestSupport.newProvisioningBridgeDTO();
        bridge.setStatus(ManagedResourceStatus.DEPROVISION);
        stubBridgesToDeployOrDelete(List.of(bridge));
        stubBridgeUpdate();

        String expectedJsonUpdateDeletingRequest =
                String.format("{\"id\": \"%s\", \"name\": \"%s\", \"endpoint\": \"%s\", \"customerId\": \"%s\", \"status\": \"deleting\"}",
                        bridge.getId(),
                        bridge.getName(),
                        bridge.getEndpoint(),
                        bridge.getCustomerId());
        String expectedJsonUpdateDeletedRequest =
                String.format("{\"id\": \"%s\", \"name\": \"%s\", \"endpoint\": \"%s\", \"customerId\": \"%s\", \"status\": \"deleted\"}",
                        bridge.getId(),
                        bridge.getName(),
                        bridge.getEndpoint(),
                        bridge.getCustomerId());

        CountDownLatch latch = new CountDownLatch(2); // Two updates to the manager are expected (1 PROVISIONING + 1 FAILED)
        addBridgeUpdateRequestListener(latch);

        managerSyncService.doBridges().await().atMost(Duration.ofSeconds(5));

        assertThat(latch.await(60, TimeUnit.SECONDS)).isTrue();
        assertJsonRequest(expectedJsonUpdateDeletingRequest, APIConstants.SHARD_API_BASE_PATH);
        assertJsonRequest(expectedJsonUpdateDeletedRequest, APIConstants.SHARD_API_BASE_PATH);
    }

    @Test
    public void testProcessorDeployment() throws JsonProcessingException, InterruptedException {
        doThrow(new IllegalStateException()).when(bridgeExecutorService).createBridgeExecutor(any());

        ProcessorDTO processor = TestSupport.newRequestedProcessorDTO();
        stubProcessorsToDeployOrDelete(Collections.singletonList(processor));
        stubProcessorUpdate();

        String expectedJsonUpdateProvisioningRequest =
                String.format("{\"id\": \"%s\", \"name\": \"%s\", \"customerId\": \"%s\", \"status\": \"provisioning\"}",
                        processor.getId(),
                        processor.getName(),
                        processor.getCustomerId());
        String expectedJsonUpdateFailedRequest =
                String.format("{\"id\": \"%s\", \"name\": \"%s\", \"customerId\": \"%s\", \"status\": \"failed\"}",
                        processor.getId(),
                        processor.getName(),
                        processor.getCustomerId());

        CountDownLatch latch = new CountDownLatch(2); // Two updates to the manager are expected (1 PROVISIONING + 1 FAILED)
        addProcessorUpdateRequestListener(latch);

        managerSyncService.doProcessors().await().atMost(Duration.ofSeconds(5));

        assertThat(latch.await(60, TimeUnit.SECONDS)).isTrue();
        assertJsonRequest(expectedJsonUpdateProvisioningRequest, APIConstants.SHARD_API_BASE_PATH + "processors");
        assertJsonRequest(expectedJsonUpdateFailedRequest, APIConstants.SHARD_API_BASE_PATH + "processors");
    }

    @Test
    public void testProcessorDeletion() throws JsonProcessingException, InterruptedException {
        doThrow(new IllegalStateException()).when(bridgeExecutorService).deleteBridgeExecutor(any());

        ProcessorDTO processor = TestSupport.newRequestedProcessorDTO();
        processor.setStatus(ManagedResourceStatus.DEPROVISION);
        stubProcessorsToDeployOrDelete(Collections.singletonList(processor));
        stubProcessorUpdate();

        String expectedJsonUpdateDeletingRequest =
                String.format("{\"id\": \"%s\", \"name\": \"%s\", \"customerId\": \"%s\", \"status\": \"deleting\"}",
                        processor.getId(),
                        processor.getName(),
                        processor.getCustomerId());
        String expectedJsonUpdateDeletedRequest =
                String.format("{\"id\": \"%s\", \"name\": \"%s\", \"customerId\": \"%s\", \"status\": \"deleted\"}",
                        processor.getId(),
                        processor.getName(),
                        processor.getCustomerId());

        CountDownLatch latch = new CountDownLatch(2); // Two updates to the manager are expected (1 PROVISIONING + 1 FAILED)
        addProcessorUpdateRequestListener(latch);

        managerSyncService.doProcessors().await().atMost(Duration.ofSeconds(5));

        assertThat(latch.await(60, TimeUnit.SECONDS)).isTrue();
        assertJsonRequest(expectedJsonUpdateDeletingRequest, APIConstants.SHARD_API_BASE_PATH + "processors");
        assertJsonRequest(expectedJsonUpdateDeletedRequest, APIConstants.SHARD_API_BASE_PATH + "processors");
    }

}
