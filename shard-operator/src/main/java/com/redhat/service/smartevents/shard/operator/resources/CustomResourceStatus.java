package com.redhat.service.smartevents.shard.operator.resources;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Common interface for Kubernetes Custom Resource status
 */
public abstract class CustomResourceStatus extends ObservedGenerationAwareStatus {

    private final Set<Condition> conditions;

    @JsonCreator
    protected CustomResourceStatus(@JsonProperty("conditions") final HashSet<Condition> initialConditions) {
        if (initialConditions == null) {
            throw new IllegalArgumentException("initialConditions can't be null");
        }
        this.conditions = initialConditions;
    }

    public final Set<Condition> getConditions(){
        return conditions;
    }

    @JsonIgnore
    public final Optional<Condition> getConditionByType(final String conditionType) {
        // o(1) operation since we are fetching by our hash key
        return conditions.stream().filter(c -> conditionType.equals(c.getType())).findFirst();
    }

    @JsonIgnore
    public final boolean isConditionTypeTrue(final String conditionType) {
        return conditions.stream().anyMatch(c -> conditionType.equals(c.getType()) && ConditionStatus.True.equals(c.getStatus()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomResourceStatus that = (CustomResourceStatus) o;
        return Objects.equals(conditions, that.conditions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conditions);
    }
}
