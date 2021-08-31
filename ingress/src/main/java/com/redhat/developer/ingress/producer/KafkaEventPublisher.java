package com.redhat.developer.ingress.producer;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.developer.ingress.api.exceptions.IngressRuntimeException;
import com.redhat.developer.ingress.api.exceptions.mappers.IngressExceptionMapper;
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

    public void sendEvent(CloudEvent cloudEvent) {
        logger.info("Sending event to event queue");
        String serializedCloudEvent;
        try {
            serializedCloudEvent = CloudEventUtils.encode(cloudEvent);
        }
        catch(RuntimeException e){
            throw new IngressRuntimeException("Failed to encode cloud event", e);
        }

        emitter.send(serializedCloudEvent);
        logger.info("Sending event to event queue - SUCCESS");
    }
}
