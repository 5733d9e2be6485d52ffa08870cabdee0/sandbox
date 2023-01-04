package com.redhat.service.smartevents.shard.operator.v2.resources;

import java.util.HashSet;
import java.util.Set;

import com.redhat.service.smartevents.shard.operator.core.resources.Condition;
import com.redhat.service.smartevents.shard.operator.core.resources.CustomResourceStatus;

public class ManagedProcessorStatus extends CustomResourceStatus {

    private static Set<Condition> getCreationConditions() {
        return new HashSet<>();
    }

    public ManagedProcessorStatus() {
        super(getCreationConditions());
    }
}
