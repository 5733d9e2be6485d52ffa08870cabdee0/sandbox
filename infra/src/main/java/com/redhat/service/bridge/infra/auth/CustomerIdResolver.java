package com.redhat.service.bridge.infra.auth;

import java.security.Principal;

public interface CustomerIdResolver {
    String resolveCustomerId(Principal principal);
}
