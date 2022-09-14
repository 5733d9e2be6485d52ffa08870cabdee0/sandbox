package com.redhat.service.smartevents.shard.operator.resources.istio.gateway;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GatewaySpecServer {

    private GatewaySpecServerPort port;
    private List<String> hosts;

    public GatewaySpecServerPort getPort() {
        return port;
    }

    public void setPort(GatewaySpecServerPort port) {
        this.port = port;
    }

    public List<String> getHosts() {
        return hosts;
    }

    public void setHosts(List<String> hosts) {
        this.hosts = hosts;
    }
}
