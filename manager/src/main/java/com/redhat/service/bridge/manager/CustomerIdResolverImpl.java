package com.redhat.service.bridge.manager;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.jwt.JsonWebToken;

import com.redhat.service.bridge.infra.api.APIConstants;

@ApplicationScoped
public class CustomerIdResolverImpl implements CustomerIdResolver {
    @Override
    public String resolveCustomerId(JsonWebToken jwt) {
        return jwt.getClaim(APIConstants.SUBJECT_ATTRIBUTE_CLAIM);
    }
}
