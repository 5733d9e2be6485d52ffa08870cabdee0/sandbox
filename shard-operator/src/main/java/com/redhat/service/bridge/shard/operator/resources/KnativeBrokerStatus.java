package com.redhat.service.bridge.shard.operator.resources;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class KnativeBrokerStatus {

    private Set<KnativeCondition> conditions;

    public Set<KnativeCondition> getConditions() {
        return conditions;
    }

    public void setConditions(Set<KnativeCondition> conditions) {
        this.conditions = conditions;
    }
}
