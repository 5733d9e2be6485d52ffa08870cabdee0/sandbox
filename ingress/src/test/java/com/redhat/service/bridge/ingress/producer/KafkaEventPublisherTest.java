package com.redhat.service.bridge.ingress.producer;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.BridgeCloudEventExtension;
import com.redhat.service.bridge.infra.utils.CloudEventUtils;
import com.redhat.service.bridge.ingress.TestUtils;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.provider.ExtensionProvider;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;

import static org.assertj.core.api.Assertions.assertThat;

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

        producer.sendEvent(bridgeId, TestUtils.buildTestCloudEventAsJsonNode());
        List<String> sentEvents = subscriber.getItems();
        assertThat(sentEvents.size()).isEqualTo(1);

        CloudEvent cloudEvent = CloudEventUtils.decode(sentEvents.get(0));
        BridgeCloudEventExtension bridgeCloudEventExtension = ExtensionProvider.getInstance().parseExtension(BridgeCloudEventExtension.class, cloudEvent);

        assertThat(bridgeCloudEventExtension.getBridgeId()).isEqualTo(bridgeId);
    }
}
