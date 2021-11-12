package com.redhat.service.bridge.ingress;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.redhat.service.bridge.ingress.producer.KafkaEventPublisher;

import io.cloudevents.CloudEvent;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        Mockito.doNothing().when(mock).sendEvent(any(String.class), any(CloudEvent.class));
        QuarkusMock.installMockForType(mock, KafkaEventPublisher.class);
    }

    @Test
    public void testSendEvent() throws JsonProcessingException {
        ingressService.processEvent(TestUtils.buildTestCloudEvent());

        verify(kafkaEventPublisher, times(1)).sendEvent(eq(TestUtils.DEFAULT_BRIDGE_ID), any(CloudEvent.class));
    }
}
