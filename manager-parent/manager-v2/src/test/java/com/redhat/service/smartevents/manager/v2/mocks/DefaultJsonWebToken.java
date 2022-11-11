package com.redhat.service.smartevents.manager.v2.mocks;

import java.util.Set;

import javax.enterprise.context.RequestScoped;

import org.eclipse.microprofile.jwt.JsonWebToken;

import com.redhat.service.smartevents.manager.v2.TestConstants;

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
