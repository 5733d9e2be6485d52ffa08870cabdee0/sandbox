package com.redhat.service.bridge.ingress.producer;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.bridge.infra.utils.CloudEventUtils;
import io.cloudevents.CloudEvent;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class KafkaEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final BroadcastProcessor<String> eventSubject = BroadcastProcessor.create();

    public void sendEvent(CloudEvent cloudEvent) {
        LOGGER.info("Sending cloudEvent with id '{}' to event queue", cloudEvent.getId());

        String serializedCloudEvent = CloudEventUtils.encode(cloudEvent);
        eventSubject.onNext(serializedCloudEvent);
        LOGGER.info("Sending cloudEvent with id '{}' to event queue - SUCCESS", cloudEvent.getId());
    }

    @Outgoing("events-out")
    public Publisher<String> getEventPublisher() {
        return eventSubject.toHotStream();
    }
}
