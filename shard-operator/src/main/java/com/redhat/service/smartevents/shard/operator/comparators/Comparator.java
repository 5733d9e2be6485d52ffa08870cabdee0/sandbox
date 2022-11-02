package com.redhat.service.smartevents.shard.operator.comparators;

import com.redhat.service.smartevents.shard.operator.resources.ResourceDelta;
import io.fabric8.kubernetes.api.model.HasMetadata;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface Comparator<T extends HasMetadata> {

    default ResourceDelta<T> compare(List<T> requestedResources, List<T> deployedResources) {

        Map<String, T> deployedMap = getObjectMap(deployedResources);
        Map<String, T> requestedMap = getObjectMap(requestedResources);

        ResourceDelta<T> resourceDelta = new ResourceDelta<>();
        for (Map.Entry<String, T> requestedEntry : requestedMap.entrySet()) {
            T deployedObject = deployedMap.get(requestedEntry.getKey());
            T requestedObject = requestedEntry.getValue();
            if (deployedObject == null) {
                resourceDelta.getCreated().add(requestedObject);
            } else if (!compare(requestedObject, deployedObject)) {
                resourceDelta.getUpdated().add(requestedObject);
            }
        }

        for (Map.Entry<String, T> deployedEntry : deployedMap.entrySet()) {
            if (!requestedMap.containsKey(deployedEntry.getKey())) {
                resourceDelta.getDeleted().add(deployedEntry.getValue());
            }
        }
        return resourceDelta;
    }

    private Map<String, T> getObjectMap(List<T> resources) {
        return resources.stream().collect(Collectors.toMap(r -> r.getMetadata().getName(), r -> r));
    }

    boolean compare(T requestedResource, T deployedResource);
}
