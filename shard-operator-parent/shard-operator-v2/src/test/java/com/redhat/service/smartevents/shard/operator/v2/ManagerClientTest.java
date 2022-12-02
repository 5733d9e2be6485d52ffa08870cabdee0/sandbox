package com.redhat.service.smartevents.shard.operator.v2;

import java.time.Duration;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.redhat.service.smartevents.infra.core.api.dto.KafkaConnectionDTO;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.shard.operator.core.EventBridgeOidcClient;
import com.redhat.service.smartevents.shard.operator.core.metrics.OperatorMetricsService;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class ManagerClientTest extends AbstractShardWireMockTest {

    @InjectMock
    EventBridgeOidcClient eventBridgeOidcClient;

    @InjectMock
    OperatorMetricsService metricsService;

    @Inject
    ManagerClientImpl managerClient;

    @BeforeEach
    public void setup() {
        Mockito.when(eventBridgeOidcClient.getToken()).thenReturn("1234");
    }

    public static final String KAFKA_BOOTSTRAP_SERVERS = "mytestkafka:9092";
    public static final String KAFKA_CLIENT_ID = "client-id";
    public static final String KAFKA_CLIENT_SECRET = "testsecret";
    public static final String KAFKA_SECURITY_PROTOCOL = "PLAINTEXT";
    public static final String KAFKA_SASL_MECHANISM = "PLAIN";
    public static final String KAFKA_TOPIC = "ob-my-id";
    public static final String KAFKA_ERROR_TOPIC = "ob-my-id-errors";

    public static final KafkaConnectionDTO KAFKA_CONNECTION_DTO = new KafkaConnectionDTO(
            KAFKA_BOOTSTRAP_SERVERS,
            KAFKA_CLIENT_ID,
            KAFKA_CLIENT_SECRET,
            KAFKA_SECURITY_PROTOCOL,
            KAFKA_SASL_MECHANISM,
            KAFKA_TOPIC,
            KAFKA_ERROR_TOPIC);

    @Test
    public void TestFetchBridgesToDeployOrDelete() throws JsonProcessingException {

        BridgeDTO updateDTO = new BridgeDTO(
                "bridgeStatusChange-1",
                "myName-1",
                "myEndpoint",
                null,
                null,
                "myCustomerId",
                "myUserName",
                KAFKA_CONNECTION_DTO,
                OperationType.CREATE);

        stubBridgesToDeployOrDelete(List.of(updateDTO));

        assertThat(managerClient.fetchBridgesToDeployOrDelete().await().atMost(Duration.ofSeconds(10)).size()).isEqualTo(1);
    }
}
