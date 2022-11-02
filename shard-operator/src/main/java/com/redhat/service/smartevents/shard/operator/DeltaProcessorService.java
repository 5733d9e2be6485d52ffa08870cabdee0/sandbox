package com.redhat.service.smartevents.shard.operator;

import com.redhat.service.smartevents.shard.operator.comparators.Comparator;
import com.redhat.service.smartevents.shard.operator.resources.BridgeIngress;
import io.fabric8.kubernetes.api.model.HasMetadata;

import java.util.List;

public interface DeltaProcessorService {
    <T extends HasMetadata> boolean processDelta(Class<T> resourceType, Comparator<T> comparator, List<T> requestedResources, List<T> deployedResources);
}
