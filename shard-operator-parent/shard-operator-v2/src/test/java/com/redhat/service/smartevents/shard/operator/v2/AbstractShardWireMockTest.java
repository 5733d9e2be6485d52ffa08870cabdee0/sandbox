package com.redhat.service.smartevents.shard.operator.v2;

import java.util.List;

import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.infra.v2.api.V2APIConstants;
import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.test.wiremock.AbstractWireMockTest;

import io.quarkus.test.common.QuarkusTestResource;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

@QuarkusTestResource(restrictToAnnotatedClass = true, value = ManagerMockResource.class)
public abstract class AbstractShardWireMockTest extends AbstractWireMockTest {

    @Inject
    protected ObjectMapper objectMapper;

    protected void stubBridgesToDeployOrDelete(List<BridgeDTO> bridgeDTOs) throws JsonProcessingException {
        wireMockServer.stubFor(get(urlEqualTo(V2APIConstants.V2_SHARD_API_BASE_PATH))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(bridgeDTOs))));
    }

    protected void stubProcessorsToDeployOrDelete(List<ProcessorDTO> processorDTOS) throws JsonProcessingException {
        wireMockServer.stubFor(get(urlEqualTo(V2APIConstants.V2_SHARD_API_BASE_PATH + "processors"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(processorDTOS))));
    }

    protected void stubBridgeUpdate() {
        wireMockServer.stubFor(put(urlEqualTo(V2APIConstants.V2_SHARD_API_BASE_PATH))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)));
    }
}
