package com.redhat.service.bridge.infra.auth;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.jwt.JsonWebToken;

import com.redhat.service.bridge.infra.api.APIConstants;

@ApplicationScoped
public class IdentityResolverImpl implements IdentityResolver {
    @Override
    public String resolve(JsonWebToken jwt) {
        return jwt.getClaim(APIConstants.SUBJECT_ATTRIBUTE_CLAIM);
    }
}
