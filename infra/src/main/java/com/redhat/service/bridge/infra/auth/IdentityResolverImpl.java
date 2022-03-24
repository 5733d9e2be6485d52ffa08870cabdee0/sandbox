package com.redhat.service.bridge.infra.auth;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.jwt.JsonWebToken;

import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.exceptions.definitions.user.ForbiddenRequestException;

@ApplicationScoped
public class IdentityResolverImpl implements IdentityResolver {
    @Override
    public String getCustomerIdFromUserToken(JsonWebToken jwt) {
        if (!jwt.containsClaim(APIConstants.ACCOUNT_ID_USER_ATTRIBUTE_CLAIM)) {
            throw new ForbiddenRequestException("Not a valid User bearer token.");
        }
        return jwt.getClaim(APIConstants.ACCOUNT_ID_USER_ATTRIBUTE_CLAIM);
    }

    @Override
    public String getCustomerIdFromServiceAccountToken(JsonWebToken jwt) {
        if (!jwt.containsClaim(APIConstants.ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)) {
            throw new ForbiddenRequestException("Not a valid Service Account bearer token.");
        }
        return jwt.getClaim(APIConstants.ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM);
    }
}
