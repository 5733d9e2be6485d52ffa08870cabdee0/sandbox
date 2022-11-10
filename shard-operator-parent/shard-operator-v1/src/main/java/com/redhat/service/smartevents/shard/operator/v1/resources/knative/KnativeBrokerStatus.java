package com.redhat.service.smartevents.shard.operator.v1.resources.knative;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.redhat.service.smartevents.shard.operator.core.resources.Condition;

@JsonIgnoreProperties(ignoreUnknown = true)
public class KnativeBrokerStatus {

    private Set<Condition> conditions;

    private Address address;

    public Set<Condition> getConditions() {
        return conditions;
    }

    public void setConditions(Set<Condition> conditions) {
        this.conditions = conditions;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public static class Address {
        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
