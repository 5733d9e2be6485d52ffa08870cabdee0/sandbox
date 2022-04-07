package com.redhat.service.bridge.shard.operator.resources.istio;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthorizationPolicySpecRuleTo {
    private AuthorizationPolicySpecRuleToOperation operation;

    public AuthorizationPolicySpecRuleToOperation getOperation() {
        return operation;
    }

    public void setOperation(AuthorizationPolicySpecRuleToOperation operation) {
        this.operation = operation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AuthorizationPolicySpecRuleTo that = (AuthorizationPolicySpecRuleTo) o;
        return Objects.equals(operation, that.operation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operation);
    }
}
