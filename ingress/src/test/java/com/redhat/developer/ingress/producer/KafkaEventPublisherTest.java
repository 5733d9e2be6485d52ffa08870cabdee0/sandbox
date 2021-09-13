package com.redhat.developer.ingress.producer;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.developer.infra.BridgeCloudEventExtension;
import com.redhat.developer.infra.utils.CloudEventUtils;
import com.redhat.developer.ingress.TestUtils;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.provider.ExtensionProvider;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;

public class KafkaEventPublisherTest {

    @BeforeEach
    public void before() {
        ExtensionProvider.getInstance().registerExtension(BridgeCloudEventExtension.class, BridgeCloudEventExtension::new);
    }

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
        BridgeCloudEventExtension bridgeCloudEventExtension = ExtensionProvider.getInstance().parseExtension(BridgeCloudEventExtension.class, cloudEvent);

        Assertions.assertEquals(bridgeId, bridgeCloudEventExtension.getBridgeId());
    }
}
