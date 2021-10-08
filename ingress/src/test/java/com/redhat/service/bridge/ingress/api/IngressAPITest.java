package com.redhat.service.bridge.ingress.api;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@QuarkusTest
public class IngressAPITest {

    @InjectMock
    KafkaEventPublisher kafkaEventPublisher;

    @Inject
    IngressService ingressService;

    @BeforeAll
    public static void setup() {
        KafkaEventPublisher mock = Mockito.mock(KafkaEventPublisher.class);
        Mockito.doNothing().when(mock).sendEvent(any(String.class), any(JsonNode.class));
        QuarkusMock.installMockForType(mock, KafkaEventPublisher.class);
    }

    @Test
    public void testSendCloudEvent() throws JsonProcessingException {
        doApiCallAfterDeploy(TestUtils.buildTestCloudEvent(), 200);
        verify(kafkaEventPublisher, times(1)).sendEvent(eq("topicName"), any(JsonNode.class));
    }

    @Test
    public void testNonCloudEvent() {
        doApiCallAfterDeploy("{\"key\": \"not a cloud event\"}", 200);
        verify(kafkaEventPublisher, times(1)).sendEvent(eq("topicName"), any(JsonNode.class));
    }

    @Test
    public void testNonJsonEvent() {
        doApiCallAfterDeploy("not a json", 400);
        verify(kafkaEventPublisher, times(0)).sendEvent(eq("topicName"), any(JsonNode.class));
    }

    @Test
    // TODO: remove after we move to k8s
    public void testSendCloudEventToUndeployedInstance() throws JsonProcessingException {
        doApiCall(TestUtils.buildTestCloudEvent(), 500);
        verify(kafkaEventPublisher, times(0)).sendEvent(eq("topicName"), any(JsonNode.class));
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
                .post("/ingress/events/topicName")
                .then().statusCode(expectedStatusCode);
    }

    private void doApiCallAfterDeploy(CloudEvent bodyEvent, int expectedStatusCode) {
        doApiCallAfterDeploy(CloudEventUtils.encode(bodyEvent), expectedStatusCode);
    }

    private void doApiCallAfterDeploy(String body, int expectedStatusCode) {
        ingressService.deploy("topicName");
        doApiCall(body, expectedStatusCode);
        ingressService.undeploy("topicName");
    }
}
