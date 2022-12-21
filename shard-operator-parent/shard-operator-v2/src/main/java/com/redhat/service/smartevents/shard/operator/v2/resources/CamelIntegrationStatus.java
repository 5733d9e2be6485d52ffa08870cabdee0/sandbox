package com.redhat.service.smartevents.shard.operator.v2.resources;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.redhat.service.smartevents.shard.operator.core.resources.Condition;
import com.redhat.service.smartevents.shard.operator.core.resources.ConditionStatus;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CamelIntegrationStatus {

    private Set<Condition> conditions = new HashSet<>();

    public Set<Condition> getConditions() {
        return conditions;
    }

    public void setConditions(Set<Condition> conditions) {
        this.conditions = conditions;
    }

    @JsonIgnore
    public final boolean isReady() {
        return conditionsAreTrue("IntegrationKitAvailable", "IntegrationPlatformAvailable", "KnativeServiceAvailable", "KnativeServiceReady");
    }

    private boolean conditionsAreTrue(String... conditionsName) {
        return Arrays.stream(conditionsName)
                .map(this::getConditionByType)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .anyMatch(c -> ConditionStatus.True.equals(c.getStatus()));
    }

    @JsonIgnore
    public final Optional<Condition> getConditionByType(final String conditionType) {
        // o(1) operation since we are fetching by our hash key
        return conditions.stream().filter(c -> conditionType.equals(c.getType())).findFirst();
    }
}
