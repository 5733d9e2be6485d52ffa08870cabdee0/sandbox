package com.redhat.service.rhose.test.wiremock;

import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.BeforeEach;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.RequestMethod;

public abstract class AbstractWireMockTest {

    @InjectWireMock
    protected WireMockServer wireMockServer;

    @BeforeEach
    protected void beforeEach() {
        wireMockServer.resetAll();
    }

    protected void addUpdateRequestListener(String expectedPath, RequestMethod expectedMethod, CountDownLatch latch) {
        wireMockServer.addMockServiceRequestListener((request, response) -> {
            if (request.getUrl().equals(expectedPath) && request.getMethod().equals(expectedMethod)) {
                latch.countDown();
            }
        });
    }
}
