package com.redhat.service.smartevents.shard.operator.v2.resources;

import java.util.Objects;
import java.util.Set;

import com.redhat.service.smartevents.shard.operator.core.resources.Condition;

/**
 * To be defined on <a href="MGDOBR-91">https://issues.redhat.com/browse/MGDOBR-91</a>
 * <p>
 * This status MUST implement the status best practices as defined by the
 * <a href="https://github.com/kubernetes/community/blob/master/contributors/devel/sig-architecture/api-conventions.md#typical-status-properties">Kubernetes API Conventions</a>
 */
public class ManagedBridgeStatus {

    private Set<Condition> conditions;

    public Set<Condition> getConditions() {
        return conditions;
    }

    public void setConditions(Set<Condition> conditions) {
        this.conditions = conditions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ManagedBridgeStatus that = (ManagedBridgeStatus) o;
        return Objects.equals(conditions, that.conditions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conditions);
    }
}
