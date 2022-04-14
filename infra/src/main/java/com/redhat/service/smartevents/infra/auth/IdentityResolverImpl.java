package com.redhat.service.smartevents.infra.auth;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.api.APIConstants;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ForbiddenRequestException;

@ApplicationScoped
public class IdentityResolverImpl implements IdentityResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdentityResolverImpl.class);

    @Override
    public String resolve(JsonWebToken jwt) {
        if (jwt.containsClaim(APIConstants.ACCOUNT_ID_USER_ATTRIBUTE_CLAIM)) {
            String customerId = jwt.getClaim(APIConstants.ACCOUNT_ID_USER_ATTRIBUTE_CLAIM);
            LOGGER.debug("User {} has been authenticated with a user account successfully", customerId);
            return customerId;
        }
        if (jwt.containsClaim(APIConstants.ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)) {
            String customerId = jwt.getClaim(APIConstants.ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM);
            LOGGER.debug("User {} has been authenticated with a service account successfully", customerId);
            return customerId;
        }
        throw new ForbiddenRequestException(String.format("The token is valid but it does not contain '%s' nor '%s' claim.", APIConstants.ACCOUNT_ID_USER_ATTRIBUTE_CLAIM,
                APIConstants.ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM));
    }
}
