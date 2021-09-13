package com.redhat.service.bridge.ingress.producer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.infra.BridgeCloudEventExtension;
import com.redhat.service.bridge.infra.utils.CloudEventUtils;
import com.redhat.service.bridge.infra.utils.exceptions.CloudEventSerializationException;
import com.redhat.service.bridge.ingress.api.exceptions.IngressException;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.provider.ExtensionProvider;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;

@ApplicationScoped
public class KafkaEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final BroadcastProcessor<String> eventSubject = BroadcastProcessor.create();

    public void init(@Observes StartupEvent e) {
        ExtensionProvider.getInstance().registerExtension(BridgeCloudEventExtension.class, BridgeCloudEventExtension::new);
    }

    /*
     * Add our specific metadata to the incoming event
     */
    private CloudEvent addMetadataToIncomingEvent(String bridgeId, CloudEvent cloudEvent) {
        return CloudEventBuilder.v1(cloudEvent)
                .withExtension(new BridgeCloudEventExtension(bridgeId))
                .build();
    }

    public void sendEvent(String bridgeId, CloudEvent cloudEvent) {
        LOGGER.info("[ingress] Sending cloudEvent with id '{}' for bridge '{}' to event queue", cloudEvent.getId(), bridgeId);

        cloudEvent = addMetadataToIncomingEvent(bridgeId, cloudEvent);
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
