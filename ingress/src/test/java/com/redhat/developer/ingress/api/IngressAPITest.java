package com.redhat.developer.ingress.api;

import java.net.URI;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.developer.infra.utils.CloudEventUtils;
import com.redhat.developer.ingress.IngressService;
import com.redhat.developer.ingress.producer.KafkaEventPublisher;

import io.cloudevents.CloudEvent;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
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
        Mockito.doNothing().when(mock).sendEvent(any(CloudEvent.class));
        QuarkusMock.installMockForType(mock, KafkaEventPublisher.class);
    }

    @Test
    public void testSendCloudEvent() throws JsonProcessingException {
        ingressService.deploy("topicName");

        given()
                .filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .when()
                .body(buildEvent())
                .post("/ingress/events/topicName")
                .then().statusCode(200);

        verify(kafkaEventPublisher, times(1)).sendEvent(any(CloudEvent.class));
    }

    @Test
    // TODO: remove after we move to k8s
    public void testSendCloudEventToUndeployedInstance() throws JsonProcessingException {
        given()
                .filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .when()
                .body(buildEvent())
                .post("/ingress/events/topicName")
                .then().statusCode(400);

        verify(kafkaEventPublisher, times(0)).sendEvent(any(CloudEvent.class));
    }

    private String buildEvent() throws JsonProcessingException {
        String jsonString = "{\"k1\":\"v1\",\"k2\":\"v2\"}";
        return CloudEventUtils.encode(
                CloudEventUtils.build("myId", "myTopic", URI.create("mySource"), "subject", new ObjectMapper().readTree(jsonString)));
    }
}
