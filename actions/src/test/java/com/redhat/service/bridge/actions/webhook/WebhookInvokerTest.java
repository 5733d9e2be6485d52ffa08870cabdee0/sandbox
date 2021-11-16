package com.redhat.service.bridge.actions.webhook;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookInvokerTest {

    public static final String TEST_EVENT = "{\"specversion\":\"1.0\",\"type\":\"TestType\",\"source\":\"/test/src\",\"id\":\"1234\"}";

    @Test
    void test() throws IOException, InterruptedException {
        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.enqueue(new MockResponse().setResponseCode(200));
        mockWebServer.start();

        HttpUrl serverUrl = mockWebServer.url("");

        WebhookInvoker invoker = new WebhookInvoker(serverUrl.toString(), new OkHttpClient.Builder().build());
        invoker.onEvent(TEST_EVENT);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getHeader("Content-Type"))
                .isEqualTo(WebhookInvoker.JSON.toString());
        assertThat(recordedRequest.getBody().readString(StandardCharsets.UTF_8))
                .isEqualTo(TEST_EVENT);
    }

}
