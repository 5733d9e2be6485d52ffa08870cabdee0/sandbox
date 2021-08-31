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
import com.redhat.developer.infra.dto.ConnectorDTO;
import com.redhat.developer.infra.dto.ConnectorStatusDTO;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

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
    public void testConnectorsAreDeployed() throws JsonProcessingException, InterruptedException {
        List<ConnectorDTO> connectorDTOs = new ArrayList<>();
        connectorDTOs.add(new ConnectorDTO("myId-1", "myName-1", "myEndpoint", "myCustomerId", ConnectorStatusDTO.REQUESTED));
        connectorDTOs.add(new ConnectorDTO("myId-2", "myName-2", "myEndpoint", "myCustomerId", ConnectorStatusDTO.REQUESTED));
        stubConnectorsToDeploy(connectorDTOs);
        stubConnectorUpdate();

        CountDownLatch latch = new CountDownLatch(2); // Two updates to the manager are expected
        addConnectorUpdateRequestListener(latch);

        managerSyncService.fetchAndProcessConnectorsFromManager().await().atMost(Duration.ofSeconds(5));

        Assertions.assertTrue(latch.await(30, TimeUnit.SECONDS));
    }

    @Test
    public void testNotifyConnectorStatusChange() throws InterruptedException {
        ConnectorDTO dto = new ConnectorDTO("myId-1", "myName-1", "myEndpoint", "myCustomerId", ConnectorStatusDTO.REQUESTED);
        stubConnectorUpdate();

        CountDownLatch latch = new CountDownLatch(1); // Two updates to the manager are expected
        addConnectorUpdateRequestListener(latch);

        managerSyncService.notifyConnectorStatusChange(dto).await().atMost(Duration.ofSeconds(5));

        Assertions.assertTrue(latch.await(30, TimeUnit.SECONDS));
    }

    private void stubConnectorsToDeploy(List<ConnectorDTO> connectorDTOs) throws JsonProcessingException {
        stubFor(get(urlEqualTo("/shard/connectors/toDeploy"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(new ObjectMapper().writeValueAsString(connectorDTOs))));
    }

    private void stubConnectorUpdate() {
        stubFor(post(urlEqualTo("/shard/connectors/toDeploy"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)));
    }

    private void addConnectorUpdateRequestListener(CountDownLatch latch) {
        wireMockServer.addMockServiceRequestListener(new RequestListener() {
            @Override
            public void requestReceived(Request request, Response response) {
                if (request.getUrl().equals("/shard/connectors/toDeploy") && request.getMethod().equals(RequestMethod.POST)) {
                    latch.countDown();
                }
            }
        });
    }
}
