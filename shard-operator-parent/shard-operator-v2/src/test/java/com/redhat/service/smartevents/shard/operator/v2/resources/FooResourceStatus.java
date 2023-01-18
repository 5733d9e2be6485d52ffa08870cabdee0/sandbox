package com.redhat.service.smartevents.shard.operator.v2.resources;

import java.util.HashSet;

import com.redhat.service.smartevents.infra.v2.api.models.DefaultConditions;
import com.redhat.service.smartevents.shard.operator.core.resources.Condition;
import com.redhat.service.smartevents.shard.operator.core.resources.ConditionStatus;

// used for testing purposes
public class FooResourceStatus extends BaseResourceStatus {

    public FooResourceStatus() {
        super(new HashSet<>() {
            {
                add(new Condition(DefaultConditions.CP_CONTROL_PLANE_READY_NAME, ConditionStatus.Unknown));
                add(new Condition(DefaultConditions.CP_DATA_PLANE_READY_NAME, ConditionStatus.Unknown));
            }
        });
    }
}
