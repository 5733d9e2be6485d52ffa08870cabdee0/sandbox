package com.redhat.service.smartevents.shard.operator.v1.resources.istio.virtualservice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VirtualServiceDestination {

    private String host;

    private VirtualServicePort port;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public VirtualServicePort getPort() {
        return port;
    }

    public void setPort(VirtualServicePort port) {
        this.port = port;
    }
}
