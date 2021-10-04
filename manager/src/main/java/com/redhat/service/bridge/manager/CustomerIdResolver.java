package com.redhat.service.bridge.manager;

import java.security.Principal;

public interface CustomerIdResolver {
    String resolveCustomerId(Principal principal);
}
