package com.redhat.service.smartevents.shard.operator.resources;

import java.util.HashSet;

public class BridgeExecutorStatus extends CustomResourceStatus {

    public static final String SECRET_AVAILABLE = "SecretAvailable";
    public static final String IMAGE_NAME_CORRECT = "ImageNameCorrect";
    public static final String DEPLOYMENT_AVAILABLE = "DeploymentAvailable";
    public static final String SERVICE_AVAILABLE = "ServiceAvailable";
    public static final String SERVICE_MONITOR_AVAILABLE = "ServiceMonitorAvailable";

    private static final HashSet<Condition> EXECUTOR_CONDITIONS = new HashSet<>() {
        {
            add(new Condition(ConditionTypeConstants.READY, ConditionStatus.False));
            add(new Condition(SECRET_AVAILABLE, ConditionStatus.False));
            add(new Condition(IMAGE_NAME_CORRECT, ConditionStatus.False));
            add(new Condition(DEPLOYMENT_AVAILABLE, ConditionStatus.False));
            add(new Condition(SERVICE_AVAILABLE, ConditionStatus.False));
            add(new Condition(SERVICE_MONITOR_AVAILABLE, ConditionStatus.False));
        }
    };

    public BridgeExecutorStatus() {
        super(EXECUTOR_CONDITIONS);
    }
}
