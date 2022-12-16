package com.redhat.service.smartevents.shard.operator.v2.resources;

import java.util.HashSet;

import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;
import com.redhat.service.smartevents.shard.operator.core.resources.Condition;
import com.redhat.service.smartevents.shard.operator.core.resources.ConditionStatus;
import com.redhat.service.smartevents.shard.operator.core.resources.ConditionTypeConstants;
import com.redhat.service.smartevents.shard.operator.core.resources.CustomResourceStatus;

/**
 * To be defined on <a href="MGDOBR-91">https://issues.redhat.com/browse/MGDOBR-91</a>
 * <p>
 * This status MUST implement the status best practices as defined by the
 * <a href="https://github.com/kubernetes/community/blob/master/contributors/devel/sig-architecture/api-conventions.md#typical-status-properties">Kubernetes API Conventions</a>
 */
public class ManagedBridgeStatus extends CustomResourceStatus {

    public static final String SECRET_AVAILABLE = "SecretAvailable";
    public static final String CONFIG_MAP_AVAILABLE = "ConfigMapAvailable";
    public static final String KNATIVE_BROKER_AVAILABLE = "KNativeBrokerAvailable";
    public static final String AUTHORISATION_POLICY_AVAILABLE = "AuthorisationPolicyAvailable";
    public static final String NETWORK_RESOURCE_AVAILABLE = "NetworkResourceAvailable";

    private static final HashSet<Condition> CONDITIONS = new HashSet<>() {
        {
            add(new Condition(ConditionTypeConstants.READY, ConditionStatus.Unknown));
            add(new Condition(SECRET_AVAILABLE, ConditionStatus.Unknown));
            add(new Condition(CONFIG_MAP_AVAILABLE, ConditionStatus.Unknown));
            add(new Condition(KNATIVE_BROKER_AVAILABLE, ConditionStatus.Unknown));
            add(new Condition(AUTHORISATION_POLICY_AVAILABLE, ConditionStatus.Unknown));
            add(new Condition(NETWORK_RESOURCE_AVAILABLE, ConditionStatus.Unknown));
        }
    };

    public ManagedBridgeStatus() {
        super(CONDITIONS);
    }

    @Override
    public ManagedResourceStatus inferManagedResourceStatus() {
        throw new IllegalStateException("This is not required in v2");
    }
}
