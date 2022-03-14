package com.redhat.service.bridge.shard.operator.resources;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class KnativeBrokerStatus {

    private Set<KnativeCondition> conditions;

    private Address address;

    public Set<KnativeCondition> getConditions() {
        return conditions;
    }

    public void setConditions(Set<KnativeCondition> conditions) {
        this.conditions = conditions;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public class Address {
        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
