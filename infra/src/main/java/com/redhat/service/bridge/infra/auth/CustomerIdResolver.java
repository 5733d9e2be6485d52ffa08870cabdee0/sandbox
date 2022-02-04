package com.redhat.service.bridge.infra.auth;

import org.eclipse.microprofile.jwt.JsonWebToken;

public interface CustomerIdResolver {
    String resolveCustomerId(JsonWebToken jwt);
}
