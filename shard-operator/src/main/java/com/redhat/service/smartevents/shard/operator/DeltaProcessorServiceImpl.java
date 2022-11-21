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
            LOGGER.debug("No delta found");
            return false;
        }


        resourceDelta.getCreated().forEach(resource -> kubernetesClient.resources(resourceType).inNamespace(resource.getMetadata().getNamespace()).create(resource));
        resourceDelta.getUpdated().forEach(resource -> kubernetesClient.resources(resourceType).inNamespace(resource.getMetadata().getNamespace()).createOrReplace(resource));
        resourceDelta.getDeleted().forEach(resource -> kubernetesClient.resources(resourceType).inNamespace(resource.getMetadata().getNamespace()).delete(resource));
        return true;
    }
}
