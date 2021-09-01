package com.redhat.developer.ingress;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.developer.infra.utils.CloudEventUtils;
import com.redhat.developer.ingress.api.exceptions.IngressRuntimeException;
import com.redhat.developer.ingress.producer.KafkaEventPublisher;

import io.cloudevents.CloudEvent;

@ApplicationScoped
public class IngressServiceImpl implements IngressService {

    private static final URI URI_PRODUCER = URI.create("ingreservice/IngressServiceImpl");

    private final List<String> deployments = new ArrayList<>();

    @Inject
    KafkaEventPublisher kafkaEventPublisher;

    @Override
    public void processEvent(String name, JsonNode event) {
        //TODO: remove after we move to k8s
        if (!deployments.contains(name)) {
            throw new IngressRuntimeException("Ingress with name " + name + " is not deployed.");
        }

        CloudEvent cloudEvent = CloudEventUtils.build(UUID.randomUUID().toString(), name,
                URI_PRODUCER, "subject", event);
        kafkaEventPublisher.sendEvent(cloudEvent);
    }

    // TODO: remove after we move to k8s
    @Override
    public String deploy(String name) {
        deployments.add(name);
        return "/ingress/events/" + name;
    }

    @Override
    public boolean undeploy(String name) {
        return deployments.remove(name);
    }
}
