package com.redhat.developer.manager;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CustomerIdResolverImpl implements CustomerIdResolver {

    @Override
    public String resolveCustomerId() {
        return "jrota";
    }
}
