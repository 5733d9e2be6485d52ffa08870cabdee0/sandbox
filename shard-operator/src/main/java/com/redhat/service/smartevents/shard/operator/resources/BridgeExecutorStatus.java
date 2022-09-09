package com.redhat.service.smartevents.shard.operator.resources;

import java.util.HashSet;

import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;

public class BridgeExecutorStatus extends CustomResourceStatus {

    private static final HashSet<Condition> EXECUTOR_CONDITIONS = new HashSet<>() {
        {
            add(new Condition(ConditionTypeConstants.READY, ConditionStatus.Unknown));
            add(new Condition(ConditionTypeConstants.AUGMENTATION, ConditionStatus.Unknown));
            add(new Condition(ConditionTypeConstants.PROGRESSING, ConditionStatus.Unknown));
        }
    };

    public BridgeExecutorStatus() {
        super(EXECUTOR_CONDITIONS);
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
