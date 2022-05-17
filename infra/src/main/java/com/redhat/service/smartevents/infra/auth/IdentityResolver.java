package com.redhat.service.smartevents.infra.auth;

import org.eclipse.microprofile.jwt.JsonWebToken;

public interface IdentityResolver {
    String resolve(JsonWebToken jwt);

    String resolveOrganisationId(JsonWebToken jwt);

    String resolveOwner(JsonWebToken jwt);
}
