
package com.redhat.service.smartevents.shard.operator;

import java.time.Duration;
import java.util.List;

import org.awaitility.Awaitility;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.redhat.service.smartevents.infra.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

abstract class AbstractManagerSyncServiceTest extends AbstractShardWireMockTest {

    protected BridgeDTO makeBridgeDTO(ManagedResourceStatus status, int suffix) {
        return new BridgeDTO("bridgesDeployed-" + suffix,
                "myName-" + suffix,
                TestSupport.BRIDGE_ENDPOINT,
                TestSupport.BRIDGE_TLS_CERTIFICATE,
                TestSupport.BRIDGE_TLS_KEY,
                TestSupport.CUSTOMER_ID,
                TestSupport.USER_NAME,
                status,
                TestSupport.KAFKA_CONNECTION_DTO);
    }

    protected void assertJsonRequest(String expectedJsonRequest, String url) {
        // For some reason the latch occasionally triggers sooner than the request is available on wiremock.
        // So wireMockServer.verify is unreliable and waiting loop is implemented.
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(
                        () -> {
                            List<LoggedRequest> find = wireMockServer.findAll(putRequestedFor(urlEqualTo(url)));
                            List<LoggedRequest> findAll = wireMockServer.findAll(putRequestedFor(urlEqualTo(url))
                                    .withRequestBody(equalToJson(expectedJsonRequest, true, true))
                                    .withHeader("Content-Type", equalTo("application/json")));
                            assertThat(findAll).hasSize(1);
                        });
    }

}
