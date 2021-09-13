package com.redhat.service.bridge.manager;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CustomerIdResolverImpl implements CustomerIdResolver {

    @Override
    public String resolveCustomerId() {
        return "jrota";
    }
}
