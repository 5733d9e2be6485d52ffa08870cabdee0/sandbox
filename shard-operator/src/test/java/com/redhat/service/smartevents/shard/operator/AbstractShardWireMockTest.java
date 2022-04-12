package com.redhat.service.smartevents.shard.operator;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.common.KafkaFuture;
import org.junit.jupiter.api.BeforeEach;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.redhat.service.smartevents.infra.api.APIConstants;
import com.redhat.service.smartevents.infra.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.shard.operator.utils.KubernetesResourcePatcher;
import com.redhat.service.smartevents.test.wiremock.AbstractWireMockTest;

import io.quarkus.test.common.QuarkusTestResource;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTestResource(restrictToAnnotatedClass = true, value = ManagerMockResource.class)
public abstract class AbstractShardWireMockTest extends AbstractWireMockTest {

    @Inject
    protected ManagerSyncService managerSyncService;

    @Inject
    protected KubernetesResourcePatcher kubernetesResourcePatcher;

    @Inject
    protected ObjectMapper objectMapper;

    // TODO: revisit processor tests when they will be integrated
    //    @InjectMock
    //    protected AdminClient kafkaAdmin;

    @BeforeEach
    protected void beforeEach() {
        super.beforeEach();
        kubernetesResourcePatcher.cleanUp();
    }

    protected void stubProcessorsToDeployOrDelete(List<ProcessorDTO> processorDTOS) throws JsonProcessingException {
        wireMockServer.stubFor(get(urlEqualTo(APIConstants.SHARD_API_BASE_PATH + "processors"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(processorDTOS))));
    }

    protected void stubBridgesToDeployOrDelete(List<BridgeDTO> bridgeDTOs) throws JsonProcessingException {
        wireMockServer.stubFor(get(urlEqualTo(APIConstants.SHARD_API_BASE_PATH))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(bridgeDTOs))));
    }

    protected void stubProcessorUpdate() {
        wireMockServer.stubFor(put(urlEqualTo(APIConstants.SHARD_API_BASE_PATH + "processors"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)));
    }

    protected void stubBridgeUpdate() {
        wireMockServer.stubFor(put(urlEqualTo(APIConstants.SHARD_API_BASE_PATH))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)));
    }

    // TODO: revisit processor tests when they will be integrated
    protected void stubListKafkaTopics(Set<String> topics) throws Exception {
        KafkaFuture<Set<String>> kafkaFuture = mock(KafkaFuture.class);
        when(kafkaFuture.get(any(Long.class), any(TimeUnit.class))).thenReturn(topics);

        ListTopicsResult listTopicsResult = mock(ListTopicsResult.class);
        when(listTopicsResult.names()).thenReturn(kafkaFuture);
        //        when(kafkaAdmin.listTopics()).thenReturn(listTopicsResult);
    }

    protected void addProcessorUpdateRequestListener(CountDownLatch latch) {
        addUpdateRequestListener(APIConstants.SHARD_API_BASE_PATH + "processors", RequestMethod.PUT, latch);
    }

    protected void addBridgeUpdateRequestListener(CountDownLatch latch) {
        addUpdateRequestListener(APIConstants.SHARD_API_BASE_PATH, RequestMethod.PUT, latch);
    }
}
