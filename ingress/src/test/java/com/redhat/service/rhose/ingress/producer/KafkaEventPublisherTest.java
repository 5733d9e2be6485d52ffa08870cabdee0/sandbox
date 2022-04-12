package com.redhat.service.rhose.ingress.producer;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.redhat.service.rhose.infra.utils.CloudEventUtils;
import com.redhat.service.rhose.ingress.TestUtils;

import io.cloudevents.CloudEvent;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;

import static org.assertj.core.api.Assertions.assertThat;

public class KafkaEventPublisherTest {

    @Test
    void testEventIsProduced() throws IOException {
        CloudEvent cloudEvent = TestUtils.buildTestCloudEvent();
        AssertSubscriber<String> subscriber = AssertSubscriber.create(1);

        KafkaEventPublisher producer = new KafkaEventPublisher();
        producer.getEventPublisher().subscribe(subscriber);

        producer.sendEvent(TestUtils.buildTestCloudEvent());
        List<String> sentEvents = subscriber.getItems();
        assertThat(sentEvents.size()).isEqualTo(1);

        CloudEvent retrievedCloudEvent = CloudEventUtils.decode(sentEvents.get(0));
        assertThat(retrievedCloudEvent.getId()).isEqualTo(cloudEvent.getId());
    }
}
