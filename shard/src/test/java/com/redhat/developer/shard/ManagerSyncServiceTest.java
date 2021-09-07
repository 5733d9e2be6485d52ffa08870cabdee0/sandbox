package com.redhat.developer.shard;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestListener;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.http.Response;
import com.redhat.developer.infra.dto.BridgeDTO;
import com.redhat.developer.infra.dto.BridgeStatus;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

@QuarkusTest
@QuarkusTestResource(restrictToAnnotatedClass = true, value = ManagerMockResource.class)
public class ManagerSyncServiceTest {

    @Inject
    ManagerSyncService managerSyncService;

    @InjectWireMock
    WireMockServer wireMockServer;

    @BeforeEach
    void cleanup() {
        wireMockServer.resetAll();
    }

    @Test
    public void testBridgesAreDeployed() throws JsonProcessingException, InterruptedException {
        List<BridgeDTO> bridgeDTOS = new ArrayList<>();
        bridgeDTOS.add(new BridgeDTO("myId-1", "myName-1", "myEndpoint", "myCustomerId", BridgeStatus.REQUESTED));
        bridgeDTOS.add(new BridgeDTO("myId-2", "myName-2", "myEndpoint", "myCustomerId", BridgeStatus.REQUESTED));
        stubBridgesToDeploy(bridgeDTOS);
        stubBridgeUpdate();
        String expectedJsonUpdateRequest = "{\"id\": \"myId-1\", \"name\": \"myName-1\", \"endpoint\": \"myEndpoint\", \"customerId\": \"myCustomerId\", \"status\": \"PROVISIONING\"}";

        CountDownLatch latch = new CountDownLatch(2); // Two updates to the manager are expected
        addBridgeUpdateRequestListener(latch);

        managerSyncService.fetchAndProcessBridgesFromManager().await().atMost(Duration.ofSeconds(5));

        Assertions.assertTrue(latch.await(30, TimeUnit.SECONDS));
        verify(postRequestedFor(urlEqualTo("/api/v1/shard/bridges/toDeploy"))
                .withRequestBody(equalToJson(expectedJsonUpdateRequest, true, true))
                .withHeader("Content-Type", equalTo("application/json")));
    }

    @Test
    public void testNotifyBridgeStatusChange() throws InterruptedException {
        BridgeDTO dto = new BridgeDTO("myId-1", "myName-1", "myEndpoint", "myCustomerId", BridgeStatus.PROVISIONING);
        stubBridgeUpdate();
        String expectedJsonUpdate = "{\"id\": \"myId-1\", \"name\": \"myName-1\", \"endpoint\": \"myEndpoint\", \"customerId\": \"myCustomerId\", \"status\": \"PROVISIONING\"}";

        CountDownLatch latch = new CountDownLatch(1); // Two updates to the manager are expected
        addBridgeUpdateRequestListener(latch);

        managerSyncService.notifyBridgeStatusChange(dto).await().atMost(Duration.ofSeconds(5));

        Assertions.assertTrue(latch.await(30, TimeUnit.SECONDS));
        verify(postRequestedFor(urlEqualTo("/api/v1/shard/bridges/toDeploy"))
                .withRequestBody(equalToJson(expectedJsonUpdate, true, true))
                .withHeader("Content-Type", equalTo("application/json")));
    }

    private void stubBridgesToDeploy(List<BridgeDTO> bridgeDTOS) throws JsonProcessingException {
        stubFor(get(urlEqualTo("/api/v1/shard/bridges/toDeploy"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(new ObjectMapper().writeValueAsString(bridgeDTOS))));
    }

    private void stubBridgeUpdate() {
        stubFor(post(urlEqualTo("/api/v1/shard/bridges/toDeploy"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)));
    }

    private void addBridgeUpdateRequestListener(CountDownLatch latch) {
        wireMockServer.addMockServiceRequestListener(new RequestListener() {
            @Override
            public void requestReceived(Request request, Response response) {
                if (request.getUrl().equals("/api/v1/shard/bridges/toDeploy") && request.getMethod().equals(RequestMethod.POST)) {
                    latch.countDown();
                }
            }
        });
    }
}
