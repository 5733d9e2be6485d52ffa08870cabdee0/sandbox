package com.redhat.service.smartevents.shard.operator.v2.resources;

import java.util.HashSet;

import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;
import com.redhat.service.smartevents.shard.operator.core.resources.Condition;
import com.redhat.service.smartevents.shard.operator.core.resources.ConditionStatus;
import com.redhat.service.smartevents.shard.operator.core.resources.ConditionTypeConstants;
import com.redhat.service.smartevents.shard.operator.core.resources.CustomResourceStatus;

public class ManagedProcessorStatus extends CustomResourceStatus {

    public static final String CAMEL_INTEGRATION_AVAILABLE = "CamelIntegrationAvailable";

    private static final HashSet<Condition> CONDITIONS = new HashSet<>() {
        {
            add(new Condition(ConditionTypeConstants.READY, ConditionStatus.Unknown));
            add(new Condition(CAMEL_INTEGRATION_AVAILABLE, ConditionStatus.Unknown));
        }
    };

    protected ManagedProcessorStatus() {
        super(CONDITIONS);
    }

    @Override
    public ManagedResourceStatus inferManagedResourceStatus() {
        throw new IllegalStateException("This is not required in v2");
    }
}
