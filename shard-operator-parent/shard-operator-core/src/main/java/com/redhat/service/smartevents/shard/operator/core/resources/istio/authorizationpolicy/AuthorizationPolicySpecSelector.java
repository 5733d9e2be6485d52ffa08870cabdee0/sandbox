package com.redhat.service.smartevents.shard.operator.core.resources.istio.authorizationpolicy;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthorizationPolicySpecSelector {

    private Map<String, String> matchLabels;

    public Map<String, String> getMatchLabels() {
        return matchLabels;
    }

    public void setMatchLabels(Map<String, String> matchLabels) {
        this.matchLabels = matchLabels;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AuthorizationPolicySpecSelector that = (AuthorizationPolicySpecSelector) o;
        return Objects.equals(matchLabels, that.matchLabels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(matchLabels);
    }
}
