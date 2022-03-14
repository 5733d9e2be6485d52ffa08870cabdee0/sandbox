package com.redhat.service.bridge.shard.operator.resources;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthorizationPolicySpecRule {
    private List<AuthorizationPolicySpecRuleWhen> when;

    private List<AuthorizationPolicySpecRuleTo> to;

    public List<AuthorizationPolicySpecRuleWhen> getWhen() {
        return when;
    }

    public void setWhen(List<AuthorizationPolicySpecRuleWhen> when) {
        this.when = when;
    }

    public List<AuthorizationPolicySpecRuleTo> getTo() {
        return to;
    }

    public void setTo(List<AuthorizationPolicySpecRuleTo> to) {
        this.to = to;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AuthorizationPolicySpecRule that = (AuthorizationPolicySpecRule) o;
        return Objects.equals(when, that.when) && Objects.equals(to, that.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(when, to);
    }
}
