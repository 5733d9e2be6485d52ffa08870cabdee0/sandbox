package com.redhat.service.smartevents.shard.operator.resources.istio;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthorizationPolicySpecRuleWhen {
    private String key;

    private List<String> values;

    public AuthorizationPolicySpecRuleWhen() {
    }

    public AuthorizationPolicySpecRuleWhen(String key, List<String> values) {
        this.key = key;
        this.values = values;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AuthorizationPolicySpecRuleWhen that = (AuthorizationPolicySpecRuleWhen) o;
        return Objects.equals(key, that.key) && Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, values);
    }
}
