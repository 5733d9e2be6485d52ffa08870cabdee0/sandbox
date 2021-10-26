package com.redhat.service.bridge.shard.operator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.common.KafkaFuture;
import org.junit.jupiter.api.BeforeEach;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestListener;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.http.Response;
import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.infra.models.filters.BaseFilter;
import com.redhat.service.bridge.infra.models.filters.StringEquals;

import io.quarkus.test.common.QuarkusTestResource;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTestResource(restrictToAnnotatedClass = true, value = ManagerMockResource.class)
public abstract class AbstractShardWireMockTest {

    @Inject
    protected ManagerSyncService managerSyncService;

    @Inject
    protected ObjectMapper objectMapper;

    // TODO: revisit processor tests when they will be integrated
    //    @InjectMock
    //    protected AdminClient kafkaAdmin;

    @InjectWireMock
    protected WireMockServer wireMockServer;

    @BeforeEach
    protected void beforeEach() {

        wireMockServer.resetAll();
    }

    protected ProcessorDTO createProcessor(BridgeDTO bridge, BridgeStatus requestedStatus) {

        Set<BaseFilter> filters = new HashSet<>();
        filters.add(new StringEquals("key", "value"));

        String transformationTemplate = "{\"test\": {key}}";

        BaseAction a = new BaseAction();
        a.setType(KafkaTopicAction.TYPE);
        a.setName("kafkaAction");

        Map<String, String> params = new HashMap<>();
        params.put(KafkaTopicAction.TOPIC_PARAM, "myTopic");
        a.setParameters(params);

        return new ProcessorDTO("processorId-1", "processorName-1", bridge, requestedStatus, filters, transformationTemplate, a);
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

    protected void addUpdateRequestListener(String expectedPath, CountDownLatch latch) {
        wireMockServer.addMockServiceRequestListener(new RequestListener() {
            @Override
            public void requestReceived(Request request, Response response) {
                if (request.getUrl().equals(expectedPath) && request.getMethod().equals(RequestMethod.PUT)) {
                    latch.countDown();
                }
            }
        });
    }

    protected void addProcessorUpdateRequestListener(CountDownLatch latch) {
        addUpdateRequestListener(APIConstants.SHARD_API_BASE_PATH + "processors", latch);
    }

    protected void addBridgeUpdateRequestListener(CountDownLatch latch) {
        addUpdateRequestListener(APIConstants.SHARD_API_BASE_PATH, latch);
    }
}
