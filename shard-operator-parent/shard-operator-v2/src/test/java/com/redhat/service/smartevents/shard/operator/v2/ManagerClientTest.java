package com.redhat.service.smartevents.shard.operator.v2;

import java.time.Duration;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.shard.operator.core.EventBridgeOidcClient;
import com.redhat.service.smartevents.shard.operator.v2.utils.Fixtures;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;

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
}
