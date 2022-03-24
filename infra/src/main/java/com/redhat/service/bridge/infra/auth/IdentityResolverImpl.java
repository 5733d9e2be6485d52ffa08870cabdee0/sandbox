package com.redhat.service.bridge.infra.auth;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.jwt.JsonWebToken;

import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.exceptions.definitions.user.ForbiddenRequestException;

@ApplicationScoped
public class IdentityResolverImpl implements IdentityResolver {
    @Override
    public String resolve(JsonWebToken jwt) {
        if (jwt.containsClaim(APIConstants.ACCOUNT_ID_USER_ATTRIBUTE_CLAIM)) {
            return jwt.getClaim(APIConstants.ACCOUNT_ID_USER_ATTRIBUTE_CLAIM);
        }
        if (jwt.containsClaim(APIConstants.ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)) {
            return jwt.getClaim(APIConstants.ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM);
        }
        throw new ForbiddenRequestException(String.format("The token is valid but it does not contain '%s' nor '%s' claim.", APIConstants.ACCOUNT_ID_USER_ATTRIBUTE_CLAIM,
                APIConstants.ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM));
    }
}
