package com.redhat.service.smartevents.shard.operator.core.resources.istio.requestauthentication;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestAuthenticationSpecJWTRule {

    private String issuer;

    private String jwksUri;

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        RequestAuthenticationSpecJWTRule that = (RequestAuthenticationSpecJWTRule) o;
        return Objects.equals(issuer, that.issuer) && Objects.equals(jwksUri, that.jwksUri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(issuer, jwksUri);
    }
}
