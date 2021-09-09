package com.redhat.developer.ingress.producer;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.redhat.developer.infra.CloudEventExtensions;
import com.redhat.developer.infra.utils.CloudEventUtils;
import com.redhat.developer.ingress.TestUtils;

import io.cloudevents.CloudEvent;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;

public class KafkaEventPublisherTest {

    @Test
    void testEventIsProduced() throws IOException {

        String bridgeId = "myBridge";
        AssertSubscriber<String> subscriber = AssertSubscriber.create(1);

        KafkaEventPublisher producer = new KafkaEventPublisher();
        producer.getEventPublisher().subscribe(subscriber);

        producer.sendEvent(bridgeId, TestUtils.buildTestCloudEvent());
        List<String> sentEvents = subscriber.getItems();
        Assertions.assertEquals(1, sentEvents.size());

        CloudEvent cloudEvent = CloudEventUtils.decode(sentEvents.get(0));
        Assertions.assertEquals(bridgeId, cloudEvent.getExtension(CloudEventExtensions.BRIDGE_ID_EXTENSION));
    }
}
