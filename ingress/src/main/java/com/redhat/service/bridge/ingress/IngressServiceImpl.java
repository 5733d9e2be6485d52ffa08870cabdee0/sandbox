package com.redhat.service.bridge.ingress;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.redhat.service.bridge.ingress.producer.KafkaEventPublisher;

import io.cloudevents.CloudEvent;

@ApplicationScoped
public class IngressServiceImpl implements IngressService {

    @ConfigProperty(name = "event-bridge.bridge.id")
    String bridgeId;

    @Inject
    KafkaEventPublisher kafkaEventPublisher;

    @Override
    public void processEvent(CloudEvent event) {
        kafkaEventPublisher.sendEvent(bridgeId, event);
    }
}
