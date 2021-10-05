package com.redhat.service.bridge.manager;

import java.security.Principal;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CustomerIdResolverImpl implements CustomerIdResolver {
    @Override
    public String resolveCustomerId(Principal principal) {
        return principal.getName();
    }
}
