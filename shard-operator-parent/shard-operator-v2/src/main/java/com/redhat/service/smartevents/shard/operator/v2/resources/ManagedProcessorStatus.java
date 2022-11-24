package com.redhat.service.smartevents.shard.operator.v2.resources;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.redhat.service.smartevents.infra.core.exceptions.BridgeErrorInstance;
import com.redhat.service.smartevents.shard.operator.core.resources.Condition;

public class ManagedProcessorStatus {

    public static final String CAMEL_INTEGRATION_AVAILABLE = "CamelIntegrationAvailable";

    private Set<Condition> conditions = new HashSet<>();

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

    @JsonIgnore
    public boolean isReady() {
        return true;
    }

    public void markConditionFalse(String ready) {

    }

    @JsonIgnore
    public boolean isConditionTypeFalse(String ready) {
        return false;
    }

    @JsonIgnore
    public boolean isConditionTypeTrue(String condition) {
        return false;
    }

    public void markConditionTrue(String condition) {

    }

    @JsonIgnore
    public void setStatusFromBridgeError(BridgeErrorInstance bei) {

    }

    public boolean getConditionByType(String ready) {
        return false;
    }
}
