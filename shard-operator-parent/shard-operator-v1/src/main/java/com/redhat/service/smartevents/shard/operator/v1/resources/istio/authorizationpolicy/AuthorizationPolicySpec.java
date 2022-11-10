package com.redhat.service.smartevents.shard.operator.v1.resources.istio.authorizationpolicy;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthorizationPolicySpec {
    private String action;

    private List<AuthorizationPolicySpecRule> rules;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public List<AuthorizationPolicySpecRule> getRules() {
        return rules;
    }

    public void setRules(List<AuthorizationPolicySpecRule> rules) {
        this.rules = rules;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AuthorizationPolicySpec that = (AuthorizationPolicySpec) o;
        return Objects.equals(action, that.action) && Objects.equals(rules, that.rules);
    }

    @Override
    public int hashCode() {
        return Objects.hash(action, rules);
    }
}
