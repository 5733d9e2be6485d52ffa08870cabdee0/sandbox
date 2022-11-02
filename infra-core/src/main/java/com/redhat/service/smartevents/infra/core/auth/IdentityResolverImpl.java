package com.redhat.service.smartevents.infra.core.auth;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.core.api.APIConstants;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.ForbiddenRequestException;

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

    @Override
    public String resolveOrganisationId(JsonWebToken jwt) {
        if (jwt.containsClaim(APIConstants.ORG_ID_USER_ATTRIBUTE_CLAIM)) {
            String organisationId = jwt.getClaim(APIConstants.ORG_ID_USER_ATTRIBUTE_CLAIM);
            LOGGER.debug("Organisation Id({}) with a user account successfully fetched from jwt token", organisationId);
            return organisationId;
        }

        if (jwt.containsClaim(APIConstants.ORG_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)) {
            String organisationId = jwt.getClaim(APIConstants.ORG_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM);
            LOGGER.debug("Organisation Id({}) with a service account successfully fetched from jwt token", organisationId);
            return organisationId;
        }

        throw new ForbiddenRequestException(String.format("The token is valid but it does not contain '%s' nor '%s' claim..", APIConstants.ORG_ID_USER_ATTRIBUTE_CLAIM,
                APIConstants.ORG_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM));
    }

    @Override
    public String resolveOwner(JsonWebToken jwt) {
        if (jwt.containsClaim(APIConstants.USER_NAME_ATTRIBUTE_CLAIM)) {
            String owner = jwt.getClaim(APIConstants.USER_NAME_ATTRIBUTE_CLAIM);
            LOGGER.debug("Owner '{}' has been successfully fetched from jwt claim '{}' .", owner, APIConstants.USER_NAME_ATTRIBUTE_CLAIM);
            return owner;
        }
        if (jwt.containsClaim(APIConstants.USER_NAME_ALTERNATIVE_ATTRIBUTE_CLAIM)) {
            String owner = jwt.getClaim(APIConstants.USER_NAME_ALTERNATIVE_ATTRIBUTE_CLAIM);
            LOGGER.debug("Owner '{}' has been successfully fetched from jwt claim '{}' .", owner, APIConstants.USER_NAME_ALTERNATIVE_ATTRIBUTE_CLAIM);
            return owner;
        }
        throw new ForbiddenRequestException(String.format("The token is valid but it does not contain '%s' nor '%s' claim.",
                APIConstants.USER_NAME_ATTRIBUTE_CLAIM,
                APIConstants.USER_NAME_ALTERNATIVE_ATTRIBUTE_CLAIM));
    }
}
