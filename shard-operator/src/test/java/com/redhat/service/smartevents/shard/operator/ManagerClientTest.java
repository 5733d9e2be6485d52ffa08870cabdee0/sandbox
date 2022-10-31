package com.redhat.service.smartevents.shard.operator;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.redhat.service.smartevents.infra.api.v1.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.api.v1.models.dto.ManagedResourceStatusUpdateDTO;
import com.redhat.service.smartevents.infra.api.v1.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.api.v1.models.dto.ProcessorManagedResourceStatusUpdateDTO;
import com.redhat.service.smartevents.infra.exceptions.definitions.platform.HTTPResponseException;
import com.redhat.service.smartevents.infra.metrics.MetricsOperation;
import com.redhat.service.smartevents.shard.operator.metrics.ManagerRequestStatus;
import com.redhat.service.smartevents.shard.operator.metrics.OperatorMetricsService;
import com.redhat.service.smartevents.test.resource.KeycloakResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.redhat.service.smartevents.infra.api.v1.V1APIConstants.V1_SHARD_API_BASE_PATH;
import static com.redhat.service.smartevents.infra.models.ManagedResourceStatus.PROVISIONING;
import static com.redhat.service.smartevents.shard.operator.TestSupport.KAFKA_CONNECTION_DTO;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
@WithOpenShiftTestServer
@QuarkusTestResource(value = KeycloakResource.class, restrictToAnnotatedClass = true)
public class ManagerClientTest extends AbstractShardWireMockTest {

    @Inject
    ManagerClient managerClient;

    @InjectMock
    OperatorMetricsService metricsService;

    @Test
    public void testNotifyBridgeStatusChange() throws InterruptedException {
        ManagedResourceStatusUpdateDTO updateDTO = new ManagedResourceStatusUpdateDTO("bridgeStatusChange-1", "myCustomerId", PROVISIONING);
        stubBridgeUpdate();
        String expectedJsonUpdate =
                "{\"id\": \"bridgeStatusChange-1\", \"customerId\": \"myCustomerId\", \"status\": \"provisioning\"}";

        CountDownLatch latch = new CountDownLatch(1); // One update to the manager is expected
        addBridgeUpdateRequestListener(latch);

        managerClient.notifyBridgeStatusChange(updateDTO).await().atMost(Duration.ofSeconds(5));

        assertThat(latch.await(30, SECONDS)).isTrue();
        wireMockServer.verify(putRequestedFor(urlEqualTo(V1_SHARD_API_BASE_PATH))
                .withRequestBody(equalToJson(expectedJsonUpdate, true, true))
                .withHeader("Content-Type", equalTo("application/json")));
    }

    @Test
    public void notifyProcessorStatusChange() throws Exception {
        ProcessorManagedResourceStatusUpdateDTO updateDTO = new ProcessorManagedResourceStatusUpdateDTO("processorStatusChange-1", "myCustomerId", "bridgeId", PROVISIONING);
        stubProcessorUpdate();

        CountDownLatch latch = new CountDownLatch(1); // One update to the manager is expected
        addProcessorUpdateRequestListener(latch);

        managerClient.notifyProcessorStatusChange(updateDTO).await().atMost(Duration.ofSeconds(5));

        assertThat(latch.await(60, SECONDS)).isTrue();
        wireMockServer.verify(putRequestedFor(urlEqualTo(V1_SHARD_API_BASE_PATH + "processors"))
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(updateDTO), true, true))
                .withHeader("Content-Type", equalTo("application/json")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void updateManagerRequestMetricsOnSuccess() {
        MetricsOperation operation = MetricsOperation.OPERATOR_MANAGER_FETCH;
        HttpResponse<Buffer> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        ((ManagerClientImpl) managerClient).updateManagerRequestMetricsOnSuccess(operation, response);

        verify(metricsService).updateManagerRequestMetrics(operation, ManagerRequestStatus.SUCCESS, "200");
    }

    @Test
    public void updateManagerRequestMetricsOnFailure() {
        MetricsOperation operation = MetricsOperation.OPERATOR_MANAGER_FETCH;
        Throwable error = mock(Throwable.class);
        ((ManagerClientImpl) managerClient).updateManagerRequestMetricsOnFailure(operation, error);

        verify(metricsService).updateManagerRequestMetrics(operation, ManagerRequestStatus.FAILURE, "null");
    }

    @Test
    public void updateManagerRequestMetricsOnFailureWithHTTPResponseException() {
        MetricsOperation operation = MetricsOperation.OPERATOR_MANAGER_FETCH;
        HTTPResponseException error = mock(HTTPResponseException.class);
        when(error.getStatusCode()).thenReturn(404);
        ((ManagerClientImpl) managerClient).updateManagerRequestMetricsOnFailure(operation, error);

        verify(metricsService).updateManagerRequestMetrics(operation, ManagerRequestStatus.FAILURE, "404");
    }

    @Test
    public void fetchBridgesToDeployOrDelete() throws JsonProcessingException {
        BridgeDTO updateDTO = new BridgeDTO(
                "bridgeStatusChange-1",
                "myName-1",
                "myEndpoint",
                null,
                null,
                "myCustomerId",
                "myUserName",
                PROVISIONING,
                KAFKA_CONNECTION_DTO);
        stubBridgesToDeployOrDelete(List.of(updateDTO));

        assertThat(managerClient.fetchBridgesToDeployOrDelete().await().atMost(Duration.ofSeconds(10)).size()).isEqualTo(1);
    }

    @Test
    public void fetchProcessorsToDeployOrDelete() throws JsonProcessingException {
        ProcessorDTO processor = TestSupport.newRequestedProcessorDTO();
        stubProcessorsToDeployOrDelete(List.of(processor));

        assertThat(managerClient.fetchProcessorsToDeployOrDelete().await().atMost(Duration.ofSeconds(10)).size()).isEqualTo(1);
    }
}
