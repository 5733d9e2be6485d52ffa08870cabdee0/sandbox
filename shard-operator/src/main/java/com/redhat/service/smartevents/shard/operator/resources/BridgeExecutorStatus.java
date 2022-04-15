package com.redhat.service.smartevents.shard.operator.resources;

import java.util.HashSet;

public class BridgeExecutorStatus extends CustomResourceStatus {

    private static final HashSet<Condition> EXECUTOR_CONDITIONS = new HashSet<Condition>() {
        {
            add(new Condition(ConditionType.Ready, ConditionStatus.Unknown));
            add(new Condition(ConditionType.Augmentation, ConditionStatus.Unknown));
        }
    };

    public BridgeExecutorStatus() {
        super(EXECUTOR_CONDITIONS);
    }
}
