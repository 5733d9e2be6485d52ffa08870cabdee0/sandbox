package com.redhat.service.bridge.infra.auth;

import org.eclipse.microprofile.jwt.JsonWebToken;

public interface IdentityResolver {

    String getCustomerIdFromUserToken(JsonWebToken jwt);

    String getCustomerIdFromServiceAccountToken(JsonWebToken jwt);
}
