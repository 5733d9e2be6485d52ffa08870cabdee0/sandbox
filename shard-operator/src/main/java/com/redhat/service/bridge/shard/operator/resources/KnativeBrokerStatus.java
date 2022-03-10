package com.redhat.service.bridge.shard.operator.resources;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class KnativeBrokerStatus {

    private Set<KnativeCondition> conditions;

    private String address;

    public Set<KnativeCondition> getConditions() {
        return conditions;
    }

    public void setConditions(Set<KnativeCondition> conditions) {
        this.conditions = conditions;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
