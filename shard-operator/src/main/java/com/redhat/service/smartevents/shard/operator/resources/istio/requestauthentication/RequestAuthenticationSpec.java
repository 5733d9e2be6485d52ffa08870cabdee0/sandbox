package com.redhat.service.smartevents.shard.operator.resources.istio.requestauthentication;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestAuthenticationSpec {

    private List<RequestAuthenticationSpecJWTRule> jwtRules;

    public List<RequestAuthenticationSpecJWTRule> getJwtRules() {
        return jwtRules;
    }

    public void setJwtRules(List<RequestAuthenticationSpecJWTRule> jwtRules) {
        this.jwtRules = jwtRules;
    }
}
