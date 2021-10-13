package com.redhat.service.bridge.ingress.api;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.redhat.service.bridge.infra.utils.CloudEventUtils;
import com.redhat.service.bridge.ingress.IngressService;
import com.redhat.service.bridge.ingress.TestUtils;
import com.redhat.service.bridge.ingress.producer.KafkaEventPublisher;

import io.cloudevents.CloudEvent;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.http.Headers;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@QuarkusTest
public class IngressAPITest {

    private static final String BRIDGE_ID = "bridgeId";
    private static final String HEADER_CE_SPECVERSION = "1.0";
    private static final String HEADER_CE_ID = "myId";
    private static final String HEADER_CE_TYPE = "myId";
    private static final String HEADER_CE_SOURCE = "mySource";
    private static final String HEADER_CE_SUBJECT = "mySubject";

    @InjectMock
    KafkaEventPublisher kafkaEventPublisher;

    @Inject
    IngressService ingressService;

    @BeforeAll
    public static void setup() {
        KafkaEventPublisher mock = Mockito.mock(KafkaEventPublisher.class);
        Mockito.doNothing().when(mock).sendEvent(any(String.class), any(CloudEvent.class));
        QuarkusMock.installMockForType(mock, KafkaEventPublisher.class);
    }

    @BeforeEach
    public void init() {
        ingressService.deploy(BRIDGE_ID);
    }

    @AfterEach
    public void cleanUp() {
        ingressService.undeploy(BRIDGE_ID);
    }

    @Test
    public void testSendCloudEvent() throws JsonProcessingException {
        doApiCall(TestUtils.buildTestCloudEvent(), 200);
        verify(kafkaEventPublisher, times(1)).sendEvent(eq(BRIDGE_ID), any(CloudEvent.class));
    }

    @Test
    public void testSendCloudEventWithBadRequestException() throws JsonProcessingException {
        Mockito.doCallRealMethod().when(kafkaEventPublisher).sendEvent(any(String.class), any(CloudEvent.class));
        doApiCall(TestUtils.buildTestCloudEventWithReservedAttributes(), 400);
        verify(kafkaEventPublisher, times(1)).sendEvent(eq(BRIDGE_ID), any(CloudEvent.class));
    }

    @Test
    public void testNonCloudEvent() {
        doApiCall("{\"key\": \"not a cloud event\"}", 400);
        verify(kafkaEventPublisher, times(0)).sendEvent(eq(BRIDGE_ID), any(CloudEvent.class));
    }

    @Test
    public void testPlainEndpointWithoutHeaders() {
        doPlainApiCall("{\"key\": \"value\"}", new Headers(), 400);
        verify(kafkaEventPublisher, times(0)).sendEvent(eq(BRIDGE_ID), any(CloudEvent.class));
    }

    @Test
    public void testPlainEndpoint() {
        Headers headers = buildHeaders(HEADER_CE_SPECVERSION, HEADER_CE_TYPE, HEADER_CE_ID, HEADER_CE_SOURCE, HEADER_CE_SUBJECT);
        doPlainApiCall("{\"key\": \"value\"}", headers, 200);
        verify(kafkaEventPublisher, times(1)).sendEvent(eq(BRIDGE_ID), any(CloudEvent.class));
    }

    @Test
    public void testPlainEndpointWithInvalidCloudEventSpecVersion() {
        Headers headers = buildHeaders("not-a-valid-specversion", HEADER_CE_TYPE, HEADER_CE_ID, HEADER_CE_SOURCE, HEADER_CE_SUBJECT);
        doPlainApiCall("{\"key\": \"value\"}", headers, 400);
        verify(kafkaEventPublisher, times(0)).sendEvent(eq(BRIDGE_ID), any(CloudEvent.class));
    }

    @Test
    public void testPlainEndpointWithInvalidURI() {
        Headers headers = buildHeaders(HEADER_CE_SPECVERSION, HEADER_CE_TYPE, HEADER_CE_ID, "{not-a-valid-source}", HEADER_CE_SUBJECT);
        doPlainApiCall("{\"key\": \"value\"}", headers, 400);
        verify(kafkaEventPublisher, times(0)).sendEvent(eq(BRIDGE_ID), any(CloudEvent.class));
    }

    @Test
    // TODO: remove after we move to k8s
    public void testSendCloudEventToUndeployedInstance() throws JsonProcessingException {
        ingressService.undeploy(BRIDGE_ID);
        doApiCall(TestUtils.buildTestCloudEvent(), 500);
        verify(kafkaEventPublisher, times(0)).sendEvent(eq(BRIDGE_ID), any(CloudEvent.class));
    }

    private void doApiCall(CloudEvent bodyEvent, int expectedStatusCode) {
        doApiCall(CloudEventUtils.encode(bodyEvent), expectedStatusCode);
    }

    private void doApiCall(String body, int expectedStatusCode) {
        given()
                .filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .when()
                .body(body)
                .post("/ingress/events/" + BRIDGE_ID)
                .then().statusCode(expectedStatusCode);
    }

    private void doPlainApiCall(String body, Headers headers, int expectedStatusCode) {
        given()
                .filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .headers(headers)
                .when()
                .body(body)
                .post("/ingress/events/" + BRIDGE_ID + "/plain")
                .then().statusCode(expectedStatusCode);
    }

    private Headers buildHeaders(String specVersion, String type, String id, String source, String subject) {
        return new Headers(
                new Header("ce-specversion", specVersion),
                new Header("ce-type", type),
                new Header("ce-id", id),
                new Header("ce-source", source),
                new Header("ce-subject", subject));
    }
}
