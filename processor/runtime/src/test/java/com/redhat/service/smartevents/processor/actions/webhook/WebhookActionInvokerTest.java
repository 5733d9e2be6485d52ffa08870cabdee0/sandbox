package com.redhat.service.smartevents.processor.actions.webhook;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.redhat.service.smartevents.infra.auth.AbstractOidcClient;
import com.redhat.service.smartevents.test.wiremock.AbstractWireMockTest;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
@QuarkusTestResource(restrictToAnnotatedClass = true, value = WebhookSinkMockResource.class)
class WebhookActionInvokerTest extends AbstractWireMockTest {

    public static final String TEST_EVENT = "{\"specversion\":\"1.0\",\"type\":\"TestType\",\"source\":\"/test/src\",\"id\":\"1234\"}";
    public static final String TEST_WEBHOOK_PATH = "/webhook";

    @Inject
    Vertx vertx;

    @ConfigProperty(name = "test.webhookSinkUrl")
    String webhookSinkUrl;

    @Test
    void test() throws InterruptedException {
        wireMockServer.stubFor(post(urlEqualTo(TEST_WEBHOOK_PATH)).willReturn(aResponse().withStatus(200)));

        CountDownLatch latch = new CountDownLatch(1);
        addUpdateRequestListener(TEST_WEBHOOK_PATH, RequestMethod.POST, latch);

        String testSinkEndpoint = webhookSinkUrl + TEST_WEBHOOK_PATH;
        WebhookActionInvoker invoker = new WebhookActionInvoker(testSinkEndpoint, WebClient.create(vertx));
        invoker.onEvent(TEST_EVENT);

        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();

        wireMockServer.verify(postRequestedFor(urlEqualTo(TEST_WEBHOOK_PATH))
                .withRequestBody(equalToJson(TEST_EVENT, true, true))
                .withHeader("Content-Type", equalTo("application/json")));
    }

    @Test
    void testBearerToken() throws InterruptedException {
        wireMockServer.stubFor(post(urlEqualTo(TEST_WEBHOOK_PATH)).willReturn(aResponse().withStatus(200)));

        CountDownLatch latch = new CountDownLatch(1);
        addUpdateRequestListener(TEST_WEBHOOK_PATH, RequestMethod.POST, latch);

        String testSinkEndpoint = webhookSinkUrl + TEST_WEBHOOK_PATH;
        AbstractOidcClient abstractOidcClient = mock(AbstractOidcClient.class);
        when(abstractOidcClient.getToken()).thenReturn("token");

        WebhookActionInvoker invoker = new WebhookActionInvoker(testSinkEndpoint, WebClient.create(vertx), abstractOidcClient);
        invoker.onEvent(TEST_EVENT);

        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();

        wireMockServer.verify(postRequestedFor(urlEqualTo(TEST_WEBHOOK_PATH))
                .withRequestBody(equalToJson(TEST_EVENT, true, true))
                .withHeader("Content-Type", equalTo("application/json"))
                .withHeader("Authorization", equalTo("Bearer token")));
    }
}
