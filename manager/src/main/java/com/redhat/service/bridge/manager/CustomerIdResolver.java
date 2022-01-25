package com.redhat.service.bridge.manager;

import org.eclipse.microprofile.jwt.JsonWebToken;

public interface CustomerIdResolver {
    String resolveCustomerId(JsonWebToken jwt);
}
