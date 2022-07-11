package com.redhat.service.smartevents.shard.operator;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.redhat.service.smartevents.infra.api.APIConstants;
import com.redhat.service.smartevents.infra.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.shard.operator.providers.CustomerNamespaceProvider;
import com.redhat.service.smartevents.shard.operator.providers.IstioGatewayProvider;
import com.redhat.service.smartevents.shard.operator.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.utils.KubernetesResourcePatcher;
import com.redhat.service.smartevents.test.resource.KeycloakResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@WithOpenShiftTestServer
@QuarkusTestResource(value = KeycloakResource.class, restrictToAnnotatedClass = true)
public class ManagerSyncServiceTest extends AbstractManagerSyncServiceTest {

    @Inject
    CustomerNamespaceProvider customerNamespaceProvider;

    @Inject
    KubernetesResourcePatcher kubernetesResourcePatcher;

    @Inject
    ManagerSyncServiceImpl managerSyncService;

    @Inject
    IstioGatewayProvider istioGatewayProvider;

    @Test
    @WithPrometheus
    public void testBridgesAreDeployed() throws JsonProcessingException, InterruptedException {
        doBridgeDeployment();
    }

    private void doBridgeDeployment() throws JsonProcessingException, InterruptedException {
        BridgeDTO bridge1 = makeBridgeDTO(ManagedResourceStatus.PREPARING, 1);
        BridgeDTO bridge2 = makeBridgeDTO(ManagedResourceStatus.PREPARING, 2);
        stubBridgesToDeployOrDelete(List.of(bridge1, bridge2));
        stubBridgeUpdate();
        String expectedJsonUpdateProvisioningRequest =
                String.format(
                        "{\"id\": \"%s\", \"name\": \"%s\", \"endpoint\": \"%s\", \"customerId\": \"%s\", \"status\": \"provisioning\"}",
                        bridge1.getId(),
                        bridge1.getName(),
                        bridge1.getEndpoint(),
                        bridge1.getCustomerId());
        String expectedJsonUpdateAvailableRequest =
                String.format(
                        "{\"id\": \"%s\", \"name\": \"%s\", \"endpoint\": \"https://ob-bridgesdeployed-1.apps.openbridge-test.fdvfn.p2.openshiftapps.com/ob-55029811/ob-bridgesdeployed-1\", \"customerId\": \"%s\", \"owner\": \"myUserName\", \"status\": \"ready\", \"kafkaConnection\": null}",
                        bridge1.getId(),
                        bridge1.getName(),
                        bridge1.getCustomerId());

        CountDownLatch latch = new CountDownLatch(4); // Four updates to the manager are expected (2 PROVISIONING + 2 READY)
        addBridgeUpdateRequestListener(latch);

        managerSyncService.doBridges().await().atMost(Duration.ofSeconds(5));

        String customerNamespace = customerNamespaceProvider.resolveName(TestSupport.CUSTOMER_ID);
        String firstBridgeName = BridgeIngress.resolveResourceName(bridge1.getId());
        String secondBridgeName = BridgeIngress.resolveResourceName(bridge2.getId());
        Awaitility.await()
                .atMost(Duration.ofMinutes(5))
                .pollInterval(Duration.ofSeconds(10))
                .untilAsserted(
                        () -> {
                            kubernetesResourcePatcher.patchReadyKnativeBroker(firstBridgeName, customerNamespace);
                            kubernetesResourcePatcher.patchReadyKnativeBroker(secondBridgeName, customerNamespace);
                            kubernetesResourcePatcher.patchReadyNetworkResource(firstBridgeName, istioGatewayProvider.getIstioGatewayService().getMetadata().getNamespace());
                            kubernetesResourcePatcher.patchReadyNetworkResource(secondBridgeName, istioGatewayProvider.getIstioGatewayService().getMetadata().getNamespace());
                        });

        assertThat(latch.await(60, TimeUnit.SECONDS)).isTrue();
        assertJsonRequest(expectedJsonUpdateProvisioningRequest, APIConstants.SHARD_API_BASE_PATH);
        assertJsonRequest(expectedJsonUpdateAvailableRequest, APIConstants.SHARD_API_BASE_PATH);
    }

    @Test
    @WithPrometheus
    public void testBridgesAreDeletedWhenDeployed() throws JsonProcessingException, InterruptedException {
        // First check provisioning completed
        doBridgeDeployment();

        // Second check provisioned Bridge is deleted
        BridgeDTO bridge1 = makeBridgeDTO(ManagedResourceStatus.DEPROVISION, 1);
        stubBridgesToDeployOrDelete(List.of(bridge1));
        stubBridgeUpdate();
        String expectedJsonUpdateDeprovisioningRequest = String.format("{\"id\": \"%s\", \"name\": \"%s\", \"endpoint\": \"\", \"customerId\": \"%s\", \"status\": \"deleting\"}",
                bridge1.getId(),
                bridge1.getName(),
                bridge1.getCustomerId());

        // The BridgeIngressController delete loop does not execute so only one update can be captured
        // See https://issues.redhat.com/browse/MGDOBR-128
        CountDownLatch latch = new CountDownLatch(1);
        addBridgeUpdateRequestListener(latch);

        managerSyncService.doBridges().await().atMost(Duration.ofSeconds(5));

        assertThat(latch.await(60, TimeUnit.SECONDS)).isTrue();
        assertJsonRequest(expectedJsonUpdateDeprovisioningRequest, APIConstants.SHARD_API_BASE_PATH);
    }

    @Test
    public void testBridgesAreDeletedWhenNotDeployed() throws JsonProcessingException, InterruptedException {
        BridgeDTO bridge1 = makeBridgeDTO(ManagedResourceStatus.DEPROVISION, 1);
        stubBridgesToDeployOrDelete(List.of(bridge1));
        stubBridgeUpdate();
        String expectedJsonUpdateDeprovisioningRequest = String.format("{\"id\": \"%s\", \"name\": \"%s\", \"endpoint\": \"\", \"customerId\": \"%s\", \"status\": \"deleting\"}",
                bridge1.getId(),
                bridge1.getName(),
                bridge1.getCustomerId());
        String expectedJsonUpdateRequest = String.format("{\"id\": \"%s\", \"name\": \"%s\", \"endpoint\": \"\", \"customerId\": \"%s\", \"status\": \"deleted\"}",
                bridge1.getId(),
                bridge1.getName(),
                bridge1.getCustomerId());

        // The BridgeIngressController does not need to execute if the CRD is not deployed
        CountDownLatch latch = new CountDownLatch(1);
        addBridgeUpdateRequestListener(latch);

        managerSyncService.doBridges().await().atMost(Duration.ofSeconds(5));

        assertThat(latch.await(60, TimeUnit.SECONDS)).isTrue();
        assertJsonRequest(expectedJsonUpdateDeprovisioningRequest, APIConstants.SHARD_API_BASE_PATH);
        assertJsonRequest(expectedJsonUpdateRequest, APIConstants.SHARD_API_BASE_PATH);
    }

    @Test
    @WithPrometheus
    public void testProcessorsAreDeployed() throws JsonProcessingException, InterruptedException {
        doProcessorDeployment();
    }

    private void doProcessorDeployment() throws JsonProcessingException, InterruptedException {
        ProcessorDTO processor = TestSupport.newRequestedProcessorDTO();

        stubProcessorsToDeployOrDelete(Collections.singletonList(processor));
        stubProcessorUpdate();

        CountDownLatch latch = new CountDownLatch(2); // Two updates to the manager are expected (1 provisioning + 1 ready)
        addProcessorUpdateRequestListener(latch);
        managerSyncService.doProcessors().await().atMost(Duration.ofSeconds(5));

        String customerNamespace = customerNamespaceProvider.resolveName(TestSupport.CUSTOMER_ID);
        String sanitizedName = BridgeExecutor.resolveResourceName(processor.getId());
        Awaitility.await()
                .atMost(Duration.ofMinutes(5))
                .pollInterval(Duration.ofSeconds(10))
                .untilAsserted(
                        () -> {
                            kubernetesResourcePatcher.patchReadyDeploymentAsReady(sanitizedName, customerNamespace);
                            kubernetesResourcePatcher.patchReadyService(sanitizedName, customerNamespace);
                        });
        assertThat(latch.await(60, TimeUnit.SECONDS)).isTrue();
        processor.setStatus(ManagedResourceStatus.READY);
        processor.setKafkaConnection(null); // the kafka connection is not included in the shard update for the manager

        assertJsonRequest(objectMapper.writeValueAsString(processor), APIConstants.SHARD_API_BASE_PATH + "processors");
    }

    @Test
    @WithPrometheus
    public void testProcessorsAreDeletedWhenDeployed() throws JsonProcessingException, InterruptedException {
        // First check provisioning completed
        doProcessorDeployment();

        // Second check provisioned Processor is deleted
        ProcessorDTO processor = TestSupport.newRequestedProcessorDTO();
        processor.setStatus(ManagedResourceStatus.DEPROVISION);

        stubProcessorsToDeployOrDelete(List.of(processor));
        stubProcessorUpdate();
        String expectedJsonUpdateRequestForDeprovisioning =
                String.format("{\"id\": \"%s\", \"name\": \"%s\", \"bridgeId\": \"%s\", \"customerId\": \"%s\", \"status\": \"deleting\"}",
                        processor.getId(),
                        processor.getName(),
                        processor.getBridgeId(),
                        processor.getCustomerId());

        // The BridgeExecutorController delete loop does not execute so only one update can be captured
        // See https://issues.redhat.com/browse/MGDOBR-128
        CountDownLatch latch = new CountDownLatch(1);
        addProcessorUpdateRequestListener(latch);

        managerSyncService.doProcessors().await().atMost(Duration.ofSeconds(5));

        assertThat(latch.await(60, TimeUnit.SECONDS)).isTrue();
        assertJsonRequest(expectedJsonUpdateRequestForDeprovisioning, APIConstants.SHARD_API_BASE_PATH + "processors");
    }

    @Test
    public void testProcessorsAreDeletedWhenNotDeployed() throws JsonProcessingException, InterruptedException {
        ProcessorDTO processor = TestSupport.newRequestedProcessorDTO();
        processor.setStatus(ManagedResourceStatus.DEPROVISION);

        stubProcessorsToDeployOrDelete(List.of(processor));
        stubProcessorUpdate();
        String expectedJsonUpdateRequestForDeprovisioning =
                String.format("{\"id\": \"%s\", \"name\": \"%s\", \"bridgeId\": \"%s\", \"customerId\": \"%s\", \"status\": \"deleting\"}",
                        processor.getId(),
                        processor.getName(),
                        processor.getBridgeId(),
                        processor.getCustomerId());
        String expectedJsonUpdateRequest =
                String.format("{\"id\": \"%s\", \"name\": \"%s\", \"bridgeId\": \"%s\", \"customerId\": \"%s\", \"status\": \"deleted\"}",
                        processor.getId(),
                        processor.getName(),
                        processor.getBridgeId(),
                        processor.getCustomerId());

        // The BridgeExecutorController does not need to execute if the CRD is not deployed
        CountDownLatch latch = new CountDownLatch(1);
        addProcessorUpdateRequestListener(latch);

        managerSyncService.doProcessors().await().atMost(Duration.ofSeconds(5));

        assertThat(latch.await(60, TimeUnit.SECONDS)).isTrue();
        assertJsonRequest(expectedJsonUpdateRequestForDeprovisioning, APIConstants.SHARD_API_BASE_PATH + "processors");
        assertJsonRequest(expectedJsonUpdateRequest, APIConstants.SHARD_API_BASE_PATH + "processors");
    }
}
