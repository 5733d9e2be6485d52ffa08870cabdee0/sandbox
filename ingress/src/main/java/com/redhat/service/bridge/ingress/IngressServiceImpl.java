package com.redhat.service.bridge.ingress;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.bridge.ingress.api.exceptions.IngressException;
import com.redhat.service.bridge.ingress.producer.KafkaEventPublisher;

import io.cloudevents.CloudEvent;

@ApplicationScoped
public class IngressServiceImpl implements IngressService {

    private final List<String> deployments = new ArrayList<>();

    @Inject
    KafkaEventPublisher kafkaEventPublisher;

    @Override
    public void processEvent(String id, CloudEvent event) {
        //TODO: remove after we move to k8s
        if (!deployments.contains(id)) {
            throw new IngressException("Ingress with name " + id + " is not deployed.");
        }
        kafkaEventPublisher.sendEvent(id, event);
    }

    // TODO: remove after we move to k8s
    @Override
    public String deploy(String id) {
        deployments.add(id);
        return "/ingress/events/" + id;
    }

    @Override
    public boolean undeploy(String id) {
        return deployments.remove(id);
    }
}
