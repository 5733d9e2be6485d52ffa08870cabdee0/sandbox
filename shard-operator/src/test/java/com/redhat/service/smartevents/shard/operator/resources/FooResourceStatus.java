package com.redhat.service.smartevents.shard.operator.resources;

import java.util.HashSet;

import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;

// used for testing purposes
public class FooResourceStatus extends CustomResourceStatus {

    public static final String AUGMENTATION = "Augmenting";

    public FooResourceStatus() {
        super(new HashSet<>() {
            {
                add(new Condition(ConditionTypeConstants.READY));
                add(new Condition(AUGMENTATION));
            }
        });
    }

    @Override
    public ManagedResourceStatus inferManagedResourceStatus() {
        if (isReady()) {
            return ManagedResourceStatus.READY;
        }
        if (isConditionTypeFalse(ConditionTypeConstants.READY) && isConditionTypeFalse(AUGMENTATION)) {
            return ManagedResourceStatus.FAILED;
        }
        return ManagedResourceStatus.PROVISIONING;
    }
}
