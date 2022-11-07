package com.redhat.service.smartevents.shard.operator.resources;

import java.util.HashSet;

import com.redhat.service.smartevents.infra.models.ManagedResourceStatus;

/**
 * To be defined on <a href="MGDOBR-91">https://issues.redhat.com/browse/MGDOBR-91</a>
 * <p>
 * This status MUST implement the status best practices as defined by the
 * <a href="https://github.com/kubernetes/community/blob/master/contributors/devel/sig-architecture/api-conventions.md#typical-status-properties">Kubernetes API Conventions</a>
 */
public class BridgeIngressStatus extends CustomResourceStatus {

    public static final String SECRET_AVAILABLE = "SecretAvailable";
    public static final String CONFIG_MAP_AVAILABLE = "ConfigMapAvailable";
    public static final String KNATIVE_BROKER_AVAILABLE = "KNativeBrokerAvailable";
    public static final String AUTHORISATION_POLICY_AVAILABLE = "AuthorisationPolicyAvailable";
    public static final String NETWORK_RESOURCE_AVAILABLE = "NetworkResourceAvailable";

    private static final HashSet<Condition> INGRESS_CONDITIONS = new HashSet<>() {
        {
            add(new Condition(ConditionTypeConstants.READY, ConditionStatus.UNKNOWN));
            add(new Condition(SECRET_AVAILABLE, ConditionStatus.UNKNOWN));
            add(new Condition(CONFIG_MAP_AVAILABLE, ConditionStatus.UNKNOWN));
            add(new Condition(KNATIVE_BROKER_AVAILABLE, ConditionStatus.UNKNOWN));
            add(new Condition(AUTHORISATION_POLICY_AVAILABLE, ConditionStatus.UNKNOWN));
            add(new Condition(NETWORK_RESOURCE_AVAILABLE, ConditionStatus.UNKNOWN));
        }
    };

    public BridgeIngressStatus() {
        super(INGRESS_CONDITIONS);
    }

    @Override
    public ManagedResourceStatus inferManagedResourceStatus() {
        if (isReady()) {
            return ManagedResourceStatus.READY;
        }
        if (isConditionTypeFalse(ConditionTypeConstants.READY)
                && !isConditionTypeTrue(SECRET_AVAILABLE)
                && !isConditionTypeTrue(CONFIG_MAP_AVAILABLE)
                && !isConditionTypeTrue(KNATIVE_BROKER_AVAILABLE)
                && !isConditionTypeTrue(AUTHORISATION_POLICY_AVAILABLE)
                && !isConditionTypeTrue(NETWORK_RESOURCE_AVAILABLE)) {
            return ManagedResourceStatus.FAILED;
        }
        return ManagedResourceStatus.PROVISIONING;
    }

}
