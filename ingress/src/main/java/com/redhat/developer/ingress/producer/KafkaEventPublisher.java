package com.redhat.developer.ingress.producer;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

import com.redhat.developer.infra.utils.CloudEventUtils;

import io.cloudevents.CloudEvent;

@ApplicationScoped
public class KafkaEventPublisher {
    private final Logger logger = Logger.getLogger(KafkaEventPublisher.class);

    @Inject
    @Channel("events-out")
    Emitter<String> emitter;

    public boolean sendEvent(CloudEvent cloudEvent) {
        logger.info("Sending event to event queue");
        Optional<String> serializedCloudEvent = CloudEventUtils.encode(cloudEvent);
        if (!serializedCloudEvent.isPresent()) {
            logger.info("Sending event to event queue - FAILED TO SERIALIZE");
            return false;
        }
        emitter.send(serializedCloudEvent.get());
        logger.info("Sending event to event queue - SUCCESS");
        return true;
    }
}
