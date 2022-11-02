package com.redhat.service.smartevents.shard.operator.comparators;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;

public class ConfigMapComparator implements Comparator<ConfigMap> {

    @Override
    public boolean compare(ConfigMap requestedResource, ConfigMap deployedResource) {
        return false;
    }
}
