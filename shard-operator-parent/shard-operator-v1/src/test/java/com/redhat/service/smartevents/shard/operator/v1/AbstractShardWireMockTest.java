package com.redhat.service.smartevents.shard.operator.v1;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.redhat.service.smartevents.infra.v1.api.V1APIConstants;
import com.redhat.service.smartevents.infra.v1.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.v1.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.shard.operator.v1.utils.KubernetesResourcePatcher;
import com.redhat.service.smartevents.test.wiremock.AbstractWireMockTest;

import io.quarkus.test.common.QuarkusTestResource;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

@QuarkusTestResource(restrictToAnnotatedClass = true, value = ManagerMockResource.class)
public abstract class AbstractShardWireMockTest extends AbstractWireMockTest {

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
        wireMockServer.stubFor(get(urlEqualTo(V1APIConstants.V1_SHARD_API_BASE_PATH + "processors"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(processorDTOS))));
    }

    protected void stubBridgesToDeployOrDelete(List<BridgeDTO> bridgeDTOs) throws JsonProcessingException {
        wireMockServer.stubFor(get(urlEqualTo(V1APIConstants.V1_SHARD_API_BASE_PATH))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(bridgeDTOs))));
    }

    protected void stubProcessorUpdate() {
        wireMockServer.stubFor(put(urlEqualTo(V1APIConstants.V1_SHARD_API_BASE_PATH + "processors"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)));
    }

    protected void stubBridgeUpdate() {
        wireMockServer.stubFor(put(urlEqualTo(V1APIConstants.V1_SHARD_API_BASE_PATH))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)));
    }

    protected void addProcessorUpdateRequestListener(CountDownLatch latch) {
        addUpdateRequestListener(V1APIConstants.V1_SHARD_API_BASE_PATH + "processors", RequestMethod.PUT, latch);
    }

    protected void addBridgeUpdateRequestListener(CountDownLatch latch) {
        addUpdateRequestListener(V1APIConstants.V1_SHARD_API_BASE_PATH, RequestMethod.PUT, latch);
    }
}
