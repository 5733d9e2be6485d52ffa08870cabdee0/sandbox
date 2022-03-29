package com.redhat.service.bridge.ingress.api;

import org.apache.http.HttpStatus;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.utils.CloudEventUtils;
import com.redhat.service.bridge.ingress.TestConstants;
import com.redhat.service.bridge.ingress.TestUtils;
import com.redhat.service.bridge.ingress.producer.KafkaEventPublisher;

import io.cloudevents.CloudEvent;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.security.TestSecurity;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.http.Headers;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
public class IngressAPITest {

    private static final String HEADER_CE_SPECVERSION = "1.0";
    private static final String HEADER_CE_ID = "myId";
    private static final String HEADER_CE_TYPE = "myId";
    private static final String HEADER_CE_SOURCE = "mySource";
    private static final String HEADER_CE_SUBJECT = "mySubject";

    @InjectMock
    KafkaEventPublisher kafkaEventPublisher;

    @InjectMock
    JsonWebToken jwt;

    @BeforeEach
    public void cleanUp() {
        mockJwt(TestConstants.DEFAULT_CUSTOMER_ID);
    }

    @BeforeAll
    public static void setup() {
        KafkaEventPublisher mock = Mockito.mock(KafkaEventPublisher.class);
        Mockito.doNothing().when(mock).sendEvent(any(CloudEvent.class));
        QuarkusMock.installMockForType(mock, KafkaEventPublisher.class);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void testSendCloudEvent() throws JsonProcessingException {
        doApiCall(TestUtils.buildTestCloudEvent(), 200);
        verify(kafkaEventPublisher, times(1)).sendEvent(any(CloudEvent.class));
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void testNonCloudEvent() {
        doApiCall("{\"key\": \"not a cloud event\"}", 400);
        verify(kafkaEventPublisher, times(0)).sendEvent(any(CloudEvent.class));
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void testPlainEndpointWithoutHeaders() {
        doPlainApiCall("{\"key\": \"value\"}", new Headers(), 400);
        verify(kafkaEventPublisher, times(0)).sendEvent(any(CloudEvent.class));
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void testPlainEndpoint() {
        Headers headers = buildHeaders(HEADER_CE_SPECVERSION, HEADER_CE_TYPE, HEADER_CE_ID, HEADER_CE_SOURCE, HEADER_CE_SUBJECT);
        doPlainApiCall("{\"key\": \"value\"}", headers, 200);
        verify(kafkaEventPublisher, times(1)).sendEvent(any(CloudEvent.class));
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void testPlainEndpointWithInvalidCloudEventSpecVersion() {
        Headers headers = buildHeaders("not-a-valid-specversion", HEADER_CE_TYPE, HEADER_CE_ID, HEADER_CE_SOURCE, HEADER_CE_SUBJECT);
        doPlainApiCall("{\"key\": \"value\"}", headers, 400);
        verify(kafkaEventPublisher, times(0)).sendEvent(any(CloudEvent.class));
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void testPlainEndpointWithInvalidURI() {
        Headers headers = buildHeaders(HEADER_CE_SPECVERSION, HEADER_CE_TYPE, HEADER_CE_ID, "{not-a-valid-source}", HEADER_CE_SUBJECT);
        doPlainApiCall("{\"key\": \"value\"}", headers, 400);
        verify(kafkaEventPublisher, times(0)).sendEvent(any(CloudEvent.class));
    }

    @Test
    @TestSecurity(user = "hacker")
    public void testPlainEndpointWithUnauthorizedUser() {
        reset(jwt);
        mockJwt("hacker");
        Headers headers = buildHeaders(HEADER_CE_SPECVERSION, HEADER_CE_TYPE, HEADER_CE_ID, HEADER_CE_SOURCE, HEADER_CE_SUBJECT);
        doPlainApiCall("{\"key\": \"value\"}", headers, HttpStatus.SC_FORBIDDEN);
    }

    @Test
    @TestSecurity(user = "hacker")
    public void testCloudEventEndpointWithUnauthorizedUser() throws JsonProcessingException {
        reset(jwt);
        mockJwt("hacker");
        doApiCall(TestUtils.buildTestCloudEvent(), HttpStatus.SC_FORBIDDEN);
    }

    @Test
    @TestSecurity(user = "robot")
    public void testSendCloudEventFromRobotAccount() throws JsonProcessingException {
        reset(jwt);
        mockJwt("robot");
        doApiCall(TestUtils.buildTestCloudEvent(), HttpStatus.SC_OK);
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
                .post("/events")
                .then().statusCode(expectedStatusCode);
    }

    private void doPlainApiCall(String body, Headers headers, int expectedStatusCode) {
        given()
                .filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .headers(headers)
                .when()
                .body(body)
                .post("/events/plain")
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

    private void mockJwt(String user) {
        when(jwt.getClaim(APIConstants.ACCOUNT_ID_USER_ATTRIBUTE_CLAIM)).thenReturn(user);
        when(jwt.containsClaim(APIConstants.ACCOUNT_ID_USER_ATTRIBUTE_CLAIM)).thenReturn(true);
    }
}
