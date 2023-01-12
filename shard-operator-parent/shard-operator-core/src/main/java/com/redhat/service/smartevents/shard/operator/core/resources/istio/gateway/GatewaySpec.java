package com.redhat.service.smartevents.shard.operator.core.resources.istio.gateway;

import java.util.List;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        GatewaySpec that = (GatewaySpec) o;
        return Objects.equals(selector, that.selector) && Objects.equals(servers, that.servers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(selector, servers);
    }
}
