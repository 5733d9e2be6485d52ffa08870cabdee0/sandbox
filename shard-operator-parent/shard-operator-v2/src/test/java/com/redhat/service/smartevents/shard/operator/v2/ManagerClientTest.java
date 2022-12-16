package com.redhat.service.smartevents.shard.operator.v2;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeStatusDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ConditionDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.shard.operator.core.EventBridgeOidcClient;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridgeStatus;
import com.redhat.service.smartevents.shard.operator.v2.utils.Fixtures;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.redhat.service.smartevents.infra.v2.api.V2APIConstants.V2_SHARD_API_BASE_PATH;
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

        assertThat(managerClient.fetchBridgesToDeployOrDelete().await().atMost(Duration.ofSeconds(10)).size()).isEqualTo(1);
    }

    @Test
    public void TestFetchProcessorsToDeployOrDelete() throws JsonProcessingException {
        ProcessorDTO processorDTO = Fixtures.createProcessor(OperationType.CREATE);

        stubProcessorsToDeployOrDelete(List.of(processorDTO));

        assertThat(managerClient.fetchProcessorsToDeployOrDelete().await().atMost(Duration.ofSeconds(10)).size()).isEqualTo(1);
    }

    @Test
    public void TestNotifyBridgeStatus() {
        stubBridgeUpdate();
        Set<ConditionDTO> conditions = new HashSet<>();
        conditions.add(new ConditionDTO(ManagedBridgeStatus.SECRET_AVAILABLE, ConditionStatus.TRUE));
        BridgeStatusDTO bridgeStatusDTO = new BridgeStatusDTO("1", 0, conditions);

        managerClient.notifyBridgeStatus(bridgeStatusDTO).await().atMost(Duration.ofSeconds(5));
        ;

        String expectedJsonUpdate =
                "{\"id\":\"1\",\"generation\":0,\"conditions\":[{\"type\":\"SecretAvailable\",\"status\":\"True\",\"reason\":null,\"message\":null,\"error_code\":null,\"last_transition_time\":null}]}";
        wireMockServer.verify(putRequestedFor(urlEqualTo(V2_SHARD_API_BASE_PATH))
                .withRequestBody(equalToJson(expectedJsonUpdate, true, true))
                .withHeader("Content-Type", equalTo("application/json")));
    }
}
