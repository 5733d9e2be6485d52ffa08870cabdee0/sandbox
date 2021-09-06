package com.redhat.developer.ingress.producer;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.developer.infra.utils.CloudEventUtils;
import com.redhat.developer.infra.utils.exceptions.CloudEventSerializationException;
import com.redhat.developer.ingress.api.exceptions.IngressException;

import io.cloudevents.CloudEvent;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;

@ApplicationScoped
public class KafkaEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final BroadcastProcessor<String> eventSubject = BroadcastProcessor.create();

    public void sendEvent(CloudEvent cloudEvent) {
        LOGGER.info("[ingress] Sending cloudEvent with id '{}' to event queue", cloudEvent.getId());
        String serializedCloudEvent;
        try {
            serializedCloudEvent = CloudEventUtils.encode(cloudEvent);
        } catch (CloudEventSerializationException e) {
            throw new IngressException("Failed to encode cloud event", e);
        }
        eventSubject.onNext(serializedCloudEvent);
        LOGGER.info("[ingress] Sending cloudEvent with id '{}' to event queue - SUCCESS", cloudEvent.getId());
    }

    @Outgoing("events-out")
    public Publisher<String> getEventPublisher() {
        return eventSubject.toHotStream();
    }
}
