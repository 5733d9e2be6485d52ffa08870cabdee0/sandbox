package com.redhat.service.smartevents.shard.operator.v2.resources;

import java.util.Objects;
import java.util.Set;

import com.redhat.service.smartevents.shard.operator.core.resources.Condition;

public class ManagedProcessorStatus {

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
        ManagedProcessorStatus that = (ManagedProcessorStatus) o;
        return Objects.equals(conditions, that.conditions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conditions);
    }
}
