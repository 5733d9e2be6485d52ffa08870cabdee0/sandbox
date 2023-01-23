package com.redhat.service.smartevents.shard.operator.v2.resources;

import java.util.HashSet;
import java.util.Set;

import com.redhat.service.smartevents.shard.operator.core.resources.Condition;
import com.redhat.service.smartevents.shard.operator.core.resources.ConditionStatus;

public class ManagedProcessorStatus extends BaseResourceStatus {

    public static final String CAMEL_INTEGRATION_AVAILABLE = "CamelIntegrationAvailable";

    private static Set<Condition> getCreationConditions() {
        Set<Condition> conditions = new HashSet<>();
        conditions.add(new Condition(CAMEL_INTEGRATION_AVAILABLE, ConditionStatus.Unknown));
        return conditions;
    }

    protected ManagedProcessorStatus() {
        super(getCreationConditions());
    }
}
