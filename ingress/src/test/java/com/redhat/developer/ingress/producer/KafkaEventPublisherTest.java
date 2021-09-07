package com.redhat.developer.ingress.producer;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.redhat.developer.ingress.TestUtils;

import io.smallrye.mutiny.helpers.test.AssertSubscriber;

public class KafkaEventPublisherTest {

    @Test
    void testEventIsProduced() throws IOException {
        AssertSubscriber<String> subscriber = AssertSubscriber.create(1);

        KafkaEventPublisher producer = new KafkaEventPublisher();
        producer.getEventPublisher().subscribe(subscriber);

        producer.sendEvent(TestUtils.buildTestCloudEvent());

        Assertions.assertEquals(1, subscriber.getItems().size());
    }
}
