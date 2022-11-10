package com.redhat.service.smartevents.shard.operator.v1.networking;

public class NetworkResource {

    private final boolean isReady;

    private final String endpoint;

    public NetworkResource(String endpoint, boolean isReady) {
        this.endpoint = endpoint;
        this.isReady = isReady;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public boolean isReady() {
        return isReady;
    }
}