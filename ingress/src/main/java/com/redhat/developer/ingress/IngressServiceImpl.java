package com.redhat.developer.ingress;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.developer.ingress.api.exceptions.IngressException;
import com.redhat.developer.ingress.producer.KafkaEventPublisher;

import io.cloudevents.CloudEvent;

@ApplicationScoped
public class IngressServiceImpl implements IngressService {

    private final List<String> deployments = new ArrayList<>();

    @Inject
    KafkaEventPublisher kafkaEventPublisher;

    @Override
    public void processEvent(String name, CloudEvent event) {
        //TODO: remove after we move to k8s
        if (!deployments.contains(name)) {
            throw new IngressException("Ingress with name " + name + " is not deployed.");
        }
        kafkaEventPublisher.sendEvent(event);
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
