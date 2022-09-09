package com.redhat.service.smartevents.shard.operator.resources;

import java.util.HashSet;

import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;

// used for testing purposes
public class FooResourceStatus extends CustomResourceStatus {

    public FooResourceStatus() {
        super(new HashSet<>() {
            {
                add(new Condition(ConditionTypeConstants.AUGMENTATION));
                add(new Condition(ConditionTypeConstants.READY));
            }
        });
    }

    @Override
    public ManagedResourceStatus inferManagedResourceStatus() {
        if (isReady()) {
            return ManagedResourceStatus.READY;
        }
        if (isConditionTypeFalse(ConditionTypeConstants.READY) && isConditionTypeFalse(ConditionTypeConstants.AUGMENTATION)) {
            return ManagedResourceStatus.FAILED;
        }
        return ManagedResourceStatus.PROVISIONING;
    }
}
