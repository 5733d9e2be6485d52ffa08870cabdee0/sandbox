package com.redhat.service.smartevents.shard.operator.v2;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ConditionDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ResourceStatusDTO;
import com.redhat.service.smartevents.shard.operator.core.EventBridgeOidcClient;
import com.redhat.service.smartevents.shard.operator.v2.utils.Fixtures;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.redhat.service.smartevents.infra.v2.api.V2APIConstants.V2_SHARD_API_BASE_PATH;
import static com.redhat.service.smartevents.infra.v2.api.V2APIConstants.V2_SHARD_API_PROCESSORS_PATH;
import static com.redhat.service.smartevents.infra.v2.api.models.DefaultConditions.DP_SECRET_READY_NAME;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@WithOpenShiftTestServer
public class ManagerClientTest extends AbstractShardWireMockTest {

    @InjectMock
    EventBridgeOidcClient eventBridgeOidcClient;

    @Inject
    ManagerClientImpl managerClient;

    @BeforeEach
    public void setup() {
        Mockito.when(eventBridgeOidcClient.getToken()).thenReturn("1234");
    }

    @Test
    public void TestFetchBridgesToDeployOrDelete() throws JsonProcessingException {

        BridgeDTO bridgeDTO = Fixtures.createBridge(OperationType.CREATE);

        stubBridgesToDeployOrDelete(List.of(bridgeDTO));

        assertThat(managerClient.fetchBridgesForDataPlane().await().atMost(Duration.ofSeconds(10)).size()).isEqualTo(1);
    }

    @Test
    public void TestFetchProcessorsToDeployOrDelete() throws JsonProcessingException {
        ProcessorDTO processorDTO = Fixtures.createProcessor(OperationType.CREATE);

        stubProcessorsToDeployOrDelete(List.of(processorDTO));

        assertThat(managerClient.fetchProcessorsForDataPlane().await().atMost(Duration.ofSeconds(10)).size()).isEqualTo(1);
    }

    @Test
    public void TestToNotifyBridgeStatus() {

        // setup
        stubBridgeUpdate();
        List<ConditionDTO> conditions1 = List.of(new ConditionDTO(DP_SECRET_READY_NAME, ConditionStatus.TRUE, ZonedDateTime.of(2022, 12, 31, 0, 0, 0, 0, ZoneOffset.UTC)));
        ResourceStatusDTO bridgeStatusDTO1 = new ResourceStatusDTO("1", 1, conditions1);

        List<ConditionDTO> conditions2 = List.of(new ConditionDTO(DP_SECRET_READY_NAME, ConditionStatus.FALSE, ZonedDateTime.of(2022, 12, 31, 0, 0, 0, 0, ZoneOffset.UTC)));
        ResourceStatusDTO bridgeStatusDTO2 = new ResourceStatusDTO("2", 2, conditions2);

        // test
        managerClient.notifyBridgeStatus(List.of(bridgeStatusDTO1, bridgeStatusDTO2)).await().atMost(Duration.ofSeconds(5));

        // assert
        String expectedJsonUpdate =
                "[{\"id\":\"1\",\"generation\":1,\"conditions\":[{\"type\":\"SecretReady\",\"status\":\"True\",\"reason\":null,\"message\":null,\"error_code\":null,\"last_transition_time\":\"2022-12-31T00:00:00Z\"}]},{\"id\":\"2\",\"generation\":2,\"conditions\":[{\"type\":\"SecretReady\",\"status\":\"False\",\"reason\":null,\"message\":null,\"error_code\":null,\"last_transition_time\":\"2022-12-31T00:00:00Z\"}]}]";
        wireMockServer.verify(putRequestedFor(urlEqualTo(V2_SHARD_API_BASE_PATH))
                .withRequestBody(equalToJson(expectedJsonUpdate, true, true))
                .withHeader("Content-Type", equalTo("application/json")));
    }

    @Test
    public void testToNotifyProcessorStatus() {

        // setup
        stubProcessorUpdate();
        List<ConditionDTO> conditions1 = List.of(new ConditionDTO(DP_SECRET_READY_NAME, ConditionStatus.TRUE, ZonedDateTime.of(2022, 12, 31, 0, 0, 0, 0, ZoneOffset.UTC)));
        ResourceStatusDTO processorStatusDTO1 = new ResourceStatusDTO("1", 1, conditions1);

        List<ConditionDTO> conditions2 = List.of(new ConditionDTO(DP_SECRET_READY_NAME, ConditionStatus.FALSE, ZonedDateTime.of(2022, 12, 31, 0, 0, 0, 0, ZoneOffset.UTC)));
        ResourceStatusDTO processorStatusDTO2 = new ResourceStatusDTO("2", 2, conditions2);

        // test
        managerClient.notifyProcessorStatus(List.of(processorStatusDTO1, processorStatusDTO2)).await().atMost(Duration.ofSeconds(5));

        // assert
        String expectedJsonUpdate =
                "[{\"id\":\"1\",\"generation\":1,\"conditions\":[{\"type\":\"SecretReady\",\"status\":\"True\",\"reason\":null,\"message\":null,\"error_code\":null,\"last_transition_time\":\"2022-12-31T00:00:00Z\"}]},{\"id\":\"2\",\"generation\":2,\"conditions\":[{\"type\":\"SecretReady\",\"status\":\"False\",\"reason\":null,\"message\":null,\"error_code\":null,\"last_transition_time\":\"2022-12-31T00:00:00Z\"}]}]";
        wireMockServer.verify(putRequestedFor(urlEqualTo(V2_SHARD_API_PROCESSORS_PATH))
                .withRequestBody(equalToJson(expectedJsonUpdate, true, true))
                .withHeader("Content-Type", equalTo("application/json")));
    }
}
