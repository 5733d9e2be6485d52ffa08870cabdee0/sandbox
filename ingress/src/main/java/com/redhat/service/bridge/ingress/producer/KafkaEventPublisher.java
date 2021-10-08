package com.redhat.service.bridge.ingress.producer;

import java.net.URI;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.service.bridge.infra.BridgeCloudEventExtension;
import com.redhat.service.bridge.infra.utils.CloudEventUtils;
import com.redhat.service.bridge.infra.utils.exceptions.CloudEventSerializationException;
import com.redhat.service.bridge.ingress.api.exceptions.IngressException;

import io.cloudevents.CloudEvent;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;

@ApplicationScoped
public class KafkaEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final BroadcastProcessor<String> eventSubject = BroadcastProcessor.create();

    public void sendEvent(String bridgeId, JsonNode event) {
        LOGGER.info("[ingress] Sending event for bridge '{}' to event queue", bridgeId);

        // Create a cloud event envelope for the user event
        CloudEvent cloudEvent = CloudEventUtils.build(UUID.randomUUID().toString(), URI.create("ingressService"),
                "ingress", event, new BridgeCloudEventExtension(bridgeId));

        String serializedCloudEvent;
        try {
            serializedCloudEvent = CloudEventUtils.encode(cloudEvent);
        } catch (CloudEventSerializationException e) {
            throw new IngressException("Failed to encode cloud event", e);
        }
        eventSubject.onNext(serializedCloudEvent);
        LOGGER.info("[ingress] Sending cloudEvent with id '{}' for bridge '{}' to event queue - SUCCESS", cloudEvent.getId(), bridgeId);
    }

    @Outgoing("events-out")
    public Publisher<String> getEventPublisher() {
        return eventSubject.toHotStream();
    }
}
