package com.redhat.developer.ingress;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.developer.ingress.api.exceptions.IngressRuntimeException;
import com.redhat.developer.ingress.producer.KafkaEventPublisher;

import io.cloudevents.CloudEvent;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@QuarkusTest
public class IngressServiceTest {

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
    public void testSendEvent() throws JsonProcessingException {
        String jsonString = "{\"k1\":\"v1\",\"k2\":\"v2\"}";
        ingressService.deploy("topicName"); // TODO: remove after we move to k8s
        ingressService.processEvent("topicName", new ObjectMapper().readTree(jsonString));

        verify(kafkaEventPublisher, times(1)).sendEvent(any(CloudEvent.class));
        ingressService.undeploy("topicName");
    }

    @Test
    // TODO: remove after we move to k8s
    public void testSendEventToUndeployedInstance() throws JsonProcessingException {
        String jsonString = "{\"k1\":\"v1\",\"k2\":\"v2\"}";
        Assertions.assertThrows(IngressRuntimeException.class,
                () -> ingressService.processEvent("topicName", new ObjectMapper().readTree(jsonString)));
        verify(kafkaEventPublisher, times(0)).sendEvent(any(CloudEvent.class));
    }
}
