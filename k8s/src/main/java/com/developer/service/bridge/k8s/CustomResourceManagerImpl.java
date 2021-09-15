package com.developer.service.bridge.k8s;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import com.redhat.service.bridge.infra.k8s.Action;
import com.redhat.service.bridge.infra.k8s.ResourceEvent;

@ApplicationScoped
public class CustomResourceManagerImpl implements CustomResourceManager {

    private final Map<String, Object> resourcesMap = new HashMap<>();

    @Inject
    Event<ResourceEvent> event;

    @Override
    public void createOrUpdateCustomResource(String name, Object customResource, String type) {
        if (resourcesMap.containsKey(name)) {
            resourcesMap.replace(name, customResource);
        } else {
            resourcesMap.put(name, customResource);
        }

        event.fire(new ResourceEvent(type, name, Action.ADDED));
    }

    @Override
    public <T> T getCustomResource(String name, Class<T> tClass) {
        return (T) resourcesMap.get(name);
    }

    @Override
    public void deleteCustomResource(String name, String type) {
        if (resourcesMap.containsKey(name)) {
            resourcesMap.remove(name);
            event.fire(new ResourceEvent(type, name, Action.DELETED));
        }
    }
}
