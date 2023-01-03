package com.redhat.service.smartevents.shard.operator.v2.resources;

import java.util.HashSet;
import java.util.Set;

import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;
import com.redhat.service.smartevents.shard.operator.core.resources.Condition;
import com.redhat.service.smartevents.shard.operator.core.resources.ConditionStatus;
import com.redhat.service.smartevents.shard.operator.core.resources.CustomResourceStatus;

import static com.redhat.service.smartevents.infra.v2.api.models.DefaultConditions.DP_AUTHORISATION_POLICY_READY_NAME;
import static com.redhat.service.smartevents.infra.v2.api.models.DefaultConditions.DP_CONFIG_MAP_READY_NAME;
import static com.redhat.service.smartevents.infra.v2.api.models.DefaultConditions.DP_KNATIVE_BROKER_READY_NAME;
import static com.redhat.service.smartevents.infra.v2.api.models.DefaultConditions.DP_NETWORK_RESOURCE_READY_NAME;
import static com.redhat.service.smartevents.infra.v2.api.models.DefaultConditions.DP_SECRET_READY_NAME;

/**
 * To be defined on <a href="MGDOBR-91">https://issues.redhat.com/browse/MGDOBR-91</a>
 * <p>
 * This status MUST implement the status best practices as defined by the
 * <a href="https://github.com/kubernetes/community/blob/master/contributors/devel/sig-architecture/api-conventions.md#typical-status-properties">Kubernetes API Conventions</a>
 */
public class ManagedBridgeStatus extends CustomResourceStatus {

    private static Set<Condition> getCreationConditions() {
        Set<Condition> conditions = new HashSet<>();
        conditions.add(new Condition(DP_SECRET_READY_NAME, ConditionStatus.Unknown));
        conditions.add(new Condition(DP_CONFIG_MAP_READY_NAME, ConditionStatus.Unknown));
        conditions.add(new Condition(DP_KNATIVE_BROKER_READY_NAME, ConditionStatus.Unknown));
        conditions.add(new Condition(DP_AUTHORISATION_POLICY_READY_NAME, ConditionStatus.Unknown));
        conditions.add(new Condition(DP_NETWORK_RESOURCE_READY_NAME, ConditionStatus.Unknown));
        return conditions;
    }

    public ManagedBridgeStatus() {
        super(getCreationConditions());
    }

    @Override
    public ManagedResourceStatus inferManagedResourceStatus() {
        throw new IllegalStateException("This is not required in v2");
    }
}
