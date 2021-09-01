package com.redhat.developer.ingress.producer;

import java.io.IOException;
import java.net.URI;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.developer.infra.utils.CloudEventUtils;

import io.smallrye.mutiny.helpers.test.AssertSubscriber;

public class KafkaEventPublisherTest {

    @Test
    void testEventIsProduced() throws IOException {
        String jsonString = "{\"k1\":\"v1\",\"k2\":\"v2\"}";

        AssertSubscriber<String> subscriber = AssertSubscriber.create(1);

        KafkaEventPublisher producer = new KafkaEventPublisher();
        producer.getEventPublisher().subscribe(subscriber);

        producer.sendEvent(CloudEventUtils.build("myId", "myTopic", URI.create("test"), "subject", new ObjectMapper().readTree(jsonString)));

        Assertions.assertEquals(1, subscriber.getItems().size());
    }
}
