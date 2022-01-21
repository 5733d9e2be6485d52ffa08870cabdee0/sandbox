package com.redhat.service.bridge.manager;

import java.util.Set;

import javax.enterprise.context.RequestScoped;

import org.eclipse.microprofile.jwt.JsonWebToken;

@RequestScoped
public class DefaultJsonWebToken implements JsonWebToken {

    @Override
    public String getName() {
        return TestConstants.SHARD_ID;
    }

    @Override
    public Set<String> getClaimNames() {
        return null;
    }

    @Override
    public <T> T getClaim(String s) {
        return null;
    }
}
