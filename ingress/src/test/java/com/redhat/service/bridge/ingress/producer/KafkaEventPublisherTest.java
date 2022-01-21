package com.redhat.service.bridge.ingress.producer;

import java.io.IOException;
import java.util.List;

import com.redhat.service.bridge.ingress.TestUtils;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class KafkaEventPublisherTest {

    @Test
    void testEventIsProduced() throws IOException {
        AssertSubscriber<String> subscriber = AssertSubscriber.create(1);

        KafkaEventPublisher producer = new KafkaEventPublisher();
        producer.getEventPublisher().subscribe(subscriber);

        producer.sendEvent(TestUtils.buildTestCloudEvent());
        List<String> sentEvents = subscriber.getItems();
        assertThat(sentEvents.size()).isEqualTo(1);
    }
}
