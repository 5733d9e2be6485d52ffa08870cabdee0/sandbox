package com.redhat.service.bridge.infra.auth;

import java.security.Principal;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CustomerIdResolverImpl implements CustomerIdResolver {
    @Override
    public String resolveCustomerId(Principal principal) {
        return principal.getName();
    }
}
