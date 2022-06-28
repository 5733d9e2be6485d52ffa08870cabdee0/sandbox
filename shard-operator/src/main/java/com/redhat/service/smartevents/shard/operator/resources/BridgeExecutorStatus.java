package com.redhat.service.smartevents.shard.operator.resources;

import java.util.HashSet;

public class BridgeExecutorStatus extends CustomResourceStatus {

    private static final HashSet<Condition> EXECUTOR_CONDITIONS = new HashSet<Condition>() {
        {
            add(new Condition(ConditionTypeConstants.READY, ConditionStatus.Unknown));
            add(new Condition(ConditionTypeConstants.AUGMENTATION, ConditionStatus.Unknown));
        }
    };

    public BridgeExecutorStatus() {
        super(EXECUTOR_CONDITIONS);
    }
}
