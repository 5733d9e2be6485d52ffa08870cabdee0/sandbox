package com.redhat.service.smartevents.shard.operator.core.resources.istio.requestauthentication;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestAuthenticationSpec {

    private List<RequestAuthenticationSpecJWTRule> jwtRules;

    public List<RequestAuthenticationSpecJWTRule> getJwtRules() {
        return jwtRules;
    }

    public void setJwtRules(List<RequestAuthenticationSpecJWTRule> jwtRules) {
        this.jwtRules = jwtRules;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequestAuthenticationSpec that = (RequestAuthenticationSpec) o;
        return Objects.equals(jwtRules, that.jwtRules);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jwtRules);
    }
}
