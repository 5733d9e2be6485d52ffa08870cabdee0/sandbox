package com.redhat.service.smartevents.shard.operator;

import com.redhat.service.smartevents.shard.operator.comparators.Comparator;
import com.redhat.service.smartevents.shard.operator.resources.ResourceDelta;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

@ApplicationScoped
public class DeltaProcessorServiceImpl implements DeltaProcessorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeltaProcessorServiceImpl.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Override
    public <T extends HasMetadata> boolean processDelta(Class<T> resourceType, Comparator<T> comparator, List<T> requestedResources, List<T> deployedResources) {

        ResourceDelta<T> resourceDelta = comparator.compare(requestedResources, deployedResources);
        if (!resourceDelta.HasChanged()) {
            LOGGER.info("No delta found");
            return false;
        }

        kubernetesClient.resources(resourceType).create(toArray(resourceDelta.getCreated(), resourceType));
        kubernetesClient.resources(resourceType).createOrReplace(toArray(resourceDelta.getUpdated(), resourceType));
        kubernetesClient.resources(resourceType).delete(resourceDelta.getDeleted());
        return true;
    }

    private static <T> T[] toArray(List<T> resources, Class<T> resourceType) {
        T[] resourceArray = (T[]) java.lang.reflect.Array.newInstance(resourceType, resources.size());
        for (int i = 0; i < resources.size(); i++) {
            resourceArray[i] = resources.get(i);
        }
        return resourceArray;
    }
}
