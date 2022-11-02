package com.redhat.service.smartevents.shard.operator.resources;

import java.util.HashSet;

import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;

public class BridgeExecutorStatus extends CustomResourceStatus {

    public static final String SECRET_AVAILABLE = "SecretAvailable";
    public static final String IMAGE_NAME_CORRECT = "ImageNameCorrect";
    public static final String DEPLOYMENT_AVAILABLE = "DeploymentAvailable";
    public static final String SERVICE_AVAILABLE = "ServiceAvailable";
    public static final String SERVICE_MONITOR_AVAILABLE = "ServiceMonitorAvailable";

    private static final HashSet<Condition> EXECUTOR_CONDITIONS = new HashSet<>() {
        {
            add(new Condition(ConditionTypeConstants.READY, ConditionStatus.Unknown));
            add(new Condition(SECRET_AVAILABLE, ConditionStatus.Unknown));
            add(new Condition(IMAGE_NAME_CORRECT, ConditionStatus.Unknown));
            add(new Condition(DEPLOYMENT_AVAILABLE, ConditionStatus.Unknown));
            add(new Condition(SERVICE_AVAILABLE, ConditionStatus.Unknown));
            add(new Condition(SERVICE_MONITOR_AVAILABLE, ConditionStatus.Unknown));
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
        if (isConditionTypeFalse(ConditionTypeConstants.READY)
                && !isConditionTypeTrue(SECRET_AVAILABLE)
                && !isConditionTypeTrue(IMAGE_NAME_CORRECT)
                && !isConditionTypeTrue(DEPLOYMENT_AVAILABLE)
                && !isConditionTypeTrue(SERVICE_AVAILABLE)
                && !isConditionTypeTrue(SERVICE_MONITOR_AVAILABLE)) {
            return ManagedResourceStatus.FAILED;
        }
        return ManagedResourceStatus.PROVISIONING;
    }

}
