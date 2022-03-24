package com.redhat.service.bridge.infra.auth;

import org.eclipse.microprofile.jwt.JsonWebToken;

public interface IdentityResolver {
    String resolve(JsonWebToken jwt);
}
