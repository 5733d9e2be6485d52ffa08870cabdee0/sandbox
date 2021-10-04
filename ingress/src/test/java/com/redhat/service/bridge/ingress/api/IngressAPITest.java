package com.redhat.service.bridge.ingress.api;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeAll;
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
        Mockito.doNothing().when(mock).sendEvent(any(String.class), any(CloudEvent.class));
        QuarkusMock.installMockForType(mock, KafkaEventPublisher.class);
    }

    @Test
    public void testSendCloudEvent() throws JsonProcessingException {
        ingressService.deploy("topicName");

        given()
                .filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .when()
                .body(CloudEventUtils.encode(TestUtils.buildTestCloudEvent()))
                .post("/ingress/events/topicName")
                .then().statusCode(200);

        verify(kafkaEventPublisher, times(1)).sendEvent(eq("topicName"), any(CloudEvent.class));
        ingressService.undeploy("topicName");
    }

    @Test
    public void testNonCloudEvent() throws JsonProcessingException {
        ingressService.deploy("topicName");

        given()
                .filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .when()
                .body("{\"key\": \"not a cloud event\"}")
                .post("/ingress/events/topicName")
                .then().statusCode(400);

        verify(kafkaEventPublisher, times(0)).sendEvent(eq("topicName"), any(CloudEvent.class));
        ingressService.undeploy("topicName");
    }

    @Test
    // TODO: remove after we move to k8s
    public void testSendCloudEventToUndeployedInstance() throws JsonProcessingException {
        given()
                .filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .when()
                .body(CloudEventUtils.encode(TestUtils.buildTestCloudEvent()))
                .post("/ingress/events/topicName")
                .then().statusCode(500);

        verify(kafkaEventPublisher, times(0)).sendEvent(eq("topicName"), any(CloudEvent.class));
    }
}
