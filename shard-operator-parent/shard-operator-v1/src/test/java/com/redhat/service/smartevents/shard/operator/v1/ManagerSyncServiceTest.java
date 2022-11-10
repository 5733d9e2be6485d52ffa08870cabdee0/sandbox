package com.redhat.service.smartevents.shard.operator.v1;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.v1.api.V1APIConstants;
import com.redhat.service.smartevents.infra.v1.api.dto.ProcessorManagedResourceStatusUpdateDTO;
import com.redhat.service.smartevents.infra.v1.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.v1.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.shard.operator.v1.providers.CustomerNamespaceProvider;
import com.redhat.service.smartevents.shard.operator.v1.providers.IstioGatewayProvider;
import com.redhat.service.smartevents.shard.operator.v1.resources.BridgeExecutor;
import com.redhat.service.smartevents.shard.operator.v1.resources.BridgeIngress;
import com.redhat.service.smartevents.shard.operator.v1.utils.KubernetesResourcePatcher;
import com.redhat.service.smartevents.test.resource.KeycloakResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
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

    @ConfigProperty(name = "rhose.metrics-name.manager-requests-total-count")
    String managerRequestsTotalCountMetricName;

    @ConfigProperty(name = "rhose.metrics-name.operator.operation-total-count")
    String operatorTotalCountMetricName;

    @ConfigProperty(name = "rhose.metrics-name.operator.operation-success-total-count")
    String operatorTotalSuccessCountMetricName;

    @ConfigProperty(name = "rhose.metrics-name.operator.operation-failure-total-count")
    String operatorTotalFailureCountMetricName;

    @Test
    @WithPrometheus
    public void testBridgesAreDeployed() throws JsonProcessingException, InterruptedException {
        doBridgeDeployment();
    }

    private void doBridgeDeployment() throws JsonProcessingException, InterruptedException {
        doBridgeDeployment(true);
    }

    private void doBridgeDeployment(boolean isSuccessful) throws JsonProcessingException, InterruptedException {
        BridgeDTO bridge1 = makeBridgeDTO(ManagedResourceStatus.PREPARING, 1);
        BridgeDTO bridge2 = makeBridgeDTO(ManagedResourceStatus.PREPARING, 2);
        stubBridgesToDeployOrDelete(List.of(bridge1, bridge2));
        stubBridgeUpdate();
        String expectedJsonUpdateProvisioningRequest =
                String.format(
                        "{\"id\": \"%s\", \"customerId\": \"%s\", \"status\": \"provisioning\"}",
                        bridge1.getId(),
                        bridge1.getCustomerId());
        String expectedJsonUpdateAvailableRequest =
                String.format(
                        "{\"id\": \"%s\", \"customerId\": \"%s\", \"status\": \"ready\"}",
                        bridge1.getId(),
                        bridge1.getCustomerId());

        CountDownLatch latch = new CountDownLatch(4); // Four updates to the manager are expected (2 PROVISIONING + 2 READY)
        addBridgeUpdateRequestListener(latch);

        managerSyncService.doBridges().await().atMost(Duration.ofSeconds(5));

        String customerNamespace = customerNamespaceProvider.resolveName(TestSupport.CUSTOMER_ID);
        String firstBridgeName = BridgeIngress.resolveResourceName(bridge1.getId());
        String secondBridgeName = BridgeIngress.resolveResourceName(bridge2.getId());

        if (isSuccessful) {
            // If the deployment is to succeed we need to mock provisioning of the dependencies
            Awaitility.await()
                    .atMost(Duration.ofMinutes(3))
                    .pollInterval(Duration.ofSeconds(5))
                    .untilAsserted(
                            () -> {
                                kubernetesResourcePatcher.patchReadyKnativeBroker(firstBridgeName, customerNamespace);
                                kubernetesResourcePatcher.patchReadyKnativeBroker(secondBridgeName, customerNamespace);
                                kubernetesResourcePatcher.patchReadyNetworkResource(firstBridgeName, istioGatewayProvider.getIstioGatewayService().getMetadata().getNamespace());
                                kubernetesResourcePatcher.patchReadyNetworkResource(secondBridgeName, istioGatewayProvider.getIstioGatewayService().getMetadata().getNamespace());
                            });
        }

        // This time-out needs to be (at least) as long as the Controller's configured timeout
        // See application.properties#event-bridge.ingress.deployment.timeout-seconds
        assertThat(latch.await(90, TimeUnit.SECONDS)).isTrue();
        assertJsonRequest(expectedJsonUpdateProvisioningRequest, V1APIConstants.V1_SHARD_API_BASE_PATH);

        if (isSuccessful) {
            assertJsonRequest(expectedJsonUpdateAvailableRequest, V1APIConstants.V1_SHARD_API_BASE_PATH);
        }
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
        String expectedJsonUpdateDeprovisioningRequest = String.format("{\"id\": \"%s\", \"customerId\": \"%s\", \"status\": \"deleting\"}",
                bridge1.getId(),
                bridge1.getCustomerId());

        // The BridgeIngressController delete loop does not execute so only one update can be captured
        // See https://issues.redhat.com/browse/MGDOBR-128
        CountDownLatch latch = new CountDownLatch(1);
        addBridgeUpdateRequestListener(latch);

        managerSyncService.doBridges().await().atMost(Duration.ofSeconds(5));

        assertThat(latch.await(60, TimeUnit.SECONDS)).isTrue();
        assertJsonRequest(expectedJsonUpdateDeprovisioningRequest, V1APIConstants.V1_SHARD_API_BASE_PATH);
    }

    @Test
    public void testBridgesAreDeletedWhenNotDeployed() throws JsonProcessingException, InterruptedException {
        BridgeDTO bridge1 = makeBridgeDTO(ManagedResourceStatus.DEPROVISION, 1);
        stubBridgesToDeployOrDelete(List.of(bridge1));
        stubBridgeUpdate();
        String expectedJsonUpdateDeprovisioningRequest = String.format("{\"id\": \"%s\", \"customerId\": \"%s\", \"status\": \"deleting\"}",
                bridge1.getId(),
                bridge1.getCustomerId());
        String expectedJsonUpdateRequest = String.format("{\"id\": \"%s\", \"customerId\": \"%s\", \"status\": \"deleted\"}",
                bridge1.getId(),
                bridge1.getCustomerId());

        // The BridgeIngressController does not need to execute if the CRD is not deployed
        CountDownLatch latch = new CountDownLatch(1);
        addBridgeUpdateRequestListener(latch);

        managerSyncService.doBridges().await().atMost(Duration.ofSeconds(5));

        assertThat(latch.await(60, TimeUnit.SECONDS)).isTrue();
        assertJsonRequest(expectedJsonUpdateDeprovisioningRequest, V1APIConstants.V1_SHARD_API_BASE_PATH);
        assertJsonRequest(expectedJsonUpdateRequest, V1APIConstants.V1_SHARD_API_BASE_PATH);
    }

    @Test
    @WithPrometheus
    public void testProcessorsAreDeployed() throws JsonProcessingException, InterruptedException {
        doProcessorDeployment();
    }

    private void doProcessorDeployment() throws JsonProcessingException, InterruptedException {
        doProcessorDeployment(true);
    }

    private void doProcessorDeployment(boolean isSuccessful) throws JsonProcessingException, InterruptedException {
        ProcessorDTO processor = TestSupport.newRequestedProcessorDTO();

        stubProcessorsToDeployOrDelete(Collections.singletonList(processor));
        stubProcessorUpdate();

        CountDownLatch latch = new CountDownLatch(2); // Two updates to the manager are expected (1 provisioning + 1 ready)
        addProcessorUpdateRequestListener(latch);
        managerSyncService.doProcessors().await().atMost(Duration.ofSeconds(5));

        String customerNamespace = customerNamespaceProvider.resolveName(TestSupport.CUSTOMER_ID);
        String sanitizedName = BridgeExecutor.resolveResourceName(processor.getId());

        if (isSuccessful) {
            // If the deployment is to succeed we need to mock provisioning of the dependencies
            Awaitility.await()
                    .atMost(Duration.ofMinutes(3))
                    .pollInterval(Duration.ofSeconds(5))
                    .untilAsserted(
                            () -> {
                                kubernetesResourcePatcher.patchReadyDeploymentAsReady(sanitizedName, customerNamespace);
                                kubernetesResourcePatcher.patchReadyService(sanitizedName, customerNamespace);
                            });
        }

        // This time-out needs to be (at least) as long as the Controller's configured timeout
        // See application.properties#event-bridge.executor.deployment.timeout-seconds
        assertThat(latch.await(90, TimeUnit.SECONDS)).isTrue();

        if (isSuccessful) {
            assertJsonRequest(
                    objectMapper.writeValueAsString(new ProcessorManagedResourceStatusUpdateDTO(processor.getId(), processor.getCustomerId(), processor.getBridgeId(), ManagedResourceStatus.READY)),
                    V1APIConstants.V1_SHARD_API_BASE_PATH + "processors");
        }
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
                String.format("{\"id\": \"%s\", \"customerId\": \"%s\", \"bridgeId\": \"%s\", \"status\": \"deleting\"}",
                        processor.getId(),
                        processor.getCustomerId(),
                        processor.getBridgeId());

        // The BridgeExecutorController delete loop does not execute so only one update can be captured
        // See https://issues.redhat.com/browse/MGDOBR-128
        CountDownLatch latch = new CountDownLatch(1);
        addProcessorUpdateRequestListener(latch);

        managerSyncService.doProcessors().await().atMost(Duration.ofSeconds(5));

        assertThat(latch.await(60, TimeUnit.SECONDS)).isTrue();
        assertJsonRequest(expectedJsonUpdateRequestForDeprovisioning, V1APIConstants.V1_SHARD_API_BASE_PATH + "processors");
    }

    @Test
    public void testProcessorsAreDeletedWhenNotDeployed() throws JsonProcessingException, InterruptedException {
        ProcessorDTO processor = TestSupport.newRequestedProcessorDTO();
        processor.setStatus(ManagedResourceStatus.DEPROVISION);

        stubProcessorsToDeployOrDelete(List.of(processor));
        stubProcessorUpdate();
        String expectedJsonUpdateRequestForDeprovisioning =
                String.format("{\"id\": \"%s\", \"customerId\": \"%s\", \"bridgeId\": \"%s\",  \"status\": \"deleting\"}",
                        processor.getId(),
                        processor.getCustomerId(),
                        processor.getBridgeId());
        String expectedJsonUpdateRequest =
                String.format("{\"id\": \"%s\", \"customerId\": \"%s\", \"bridgeId\": \"%s\", \"status\": \"deleted\"}",
                        processor.getId(),
                        processor.getCustomerId(),
                        processor.getBridgeId());

        // The BridgeExecutorController does not need to execute if the CRD is not deployed
        CountDownLatch latch = new CountDownLatch(1);
        addProcessorUpdateRequestListener(latch);

        managerSyncService.doProcessors().await().atMost(Duration.ofSeconds(5));

        assertThat(latch.await(60, TimeUnit.SECONDS)).isTrue();
        assertJsonRequest(expectedJsonUpdateRequestForDeprovisioning, V1APIConstants.V1_SHARD_API_BASE_PATH + "processors");
        assertJsonRequest(expectedJsonUpdateRequest, V1APIConstants.V1_SHARD_API_BASE_PATH + "processors");
    }

    @Test
    public void bridgeMetricsAreProducedForSuccessfulDeployment() throws JsonProcessingException, InterruptedException {
        doBridgeDeployment();

        String metrics = given()
                .filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .when()
                .get("/q/metrics")
                .then()
                .extract()
                .body()
                .asString();

        // Not all metrics are recorded by this test. Only the "successful" path in this test.
        assertThat(metrics).contains(managerRequestsTotalCountMetricName);
        assertThat(metrics).contains(operatorTotalCountMetricName);
        assertThat(metrics).contains(operatorTotalSuccessCountMetricName);
    }

    @Test
    public void bridgeMetricsAreProducedForFailedDeployment() throws JsonProcessingException, InterruptedException {
        doBridgeDeployment(false);

        String metrics = given()
                .filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .when()
                .get("/q/metrics")
                .then()
                .extract()
                .body()
                .asString();

        // Not all metrics are recorded by this test. Only the "failure" path in this test.
        assertThat(metrics).contains(managerRequestsTotalCountMetricName);
        assertThat(metrics).contains(operatorTotalCountMetricName);
        assertThat(metrics).contains(operatorTotalFailureCountMetricName);
    }

    @Test
    public void processorMetricsAreProducedForSuccessfulDeployment() throws JsonProcessingException, InterruptedException {
        doProcessorDeployment();

        String metrics = given()
                .filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .when()
                .get("/q/metrics")
                .then()
                .extract()
                .body()
                .asString();

        // Not all metrics are recorded by this test. Only the "successful" path in this test.
        assertThat(metrics).contains(managerRequestsTotalCountMetricName);
        assertThat(metrics).contains(operatorTotalCountMetricName);
        assertThat(metrics).contains(operatorTotalSuccessCountMetricName);
    }

    @Test
    public void processorMetricsAreProducedForFailedDeployment() throws JsonProcessingException, InterruptedException {
        doProcessorDeployment(false);

        String metrics = given()
                .filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .when()
                .get("/q/metrics")
                .then()
                .extract()
                .body()
                .asString();

        // Not all metrics are recorded by this test. Only the "failure" path in this test.
        assertThat(metrics).contains(managerRequestsTotalCountMetricName);
        assertThat(metrics).contains(operatorTotalCountMetricName);
        assertThat(metrics).contains(operatorTotalFailureCountMetricName);
    }

}
