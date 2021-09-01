package com.redhat.developer.ingress.producer;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.jboss.logging.Logger;
import org.reactivestreams.Publisher;

import com.redhat.developer.infra.utils.CloudEventUtils;
import com.redhat.developer.ingress.api.exceptions.IngressRuntimeException;

import io.cloudevents.CloudEvent;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;

@ApplicationScoped
public class KafkaEventPublisher {

    private final Logger logger = Logger.getLogger(KafkaEventPublisher.class);

    private final BroadcastProcessor<String> eventSubject = BroadcastProcessor.create();

    public void sendEvent(CloudEvent cloudEvent) {
        logger.info("Sending event to event queue");
        String serializedCloudEvent;
        try {
            serializedCloudEvent = CloudEventUtils.encode(cloudEvent);
        } catch (RuntimeException e) {
            throw new IngressRuntimeException("Failed to encode cloud event", e);
        }
        eventSubject.onNext(serializedCloudEvent);
        logger.info("Sending event to event queue - SUCCESS");
    }

    @Outgoing("events-out")
    public Publisher<String> getEventPublisher() {
        return eventSubject.toHotStream();
    }
}
