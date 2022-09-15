package com.redhat.service.smartevents.shard.operator.resources.istio.gateway;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GatewaySpec {
    private GatewaySpecSelector selector;
    private List<GatewaySpecServer> servers;

    public GatewaySpecSelector getSelector() {
        return selector;
    }

    public void setSelector(GatewaySpecSelector selector) {
        this.selector = selector;
    }

    public List<GatewaySpecServer> getServers() {
        return servers;
    }

    public void setServers(List<GatewaySpecServer> servers) {
        this.servers = servers;
    }
}
