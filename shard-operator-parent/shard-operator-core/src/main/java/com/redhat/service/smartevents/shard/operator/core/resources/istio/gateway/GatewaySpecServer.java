package com.redhat.service.smartevents.shard.operator.core.resources.istio.gateway;

import java.util.List;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        GatewaySpecServer that = (GatewaySpecServer) o;
        return Objects.equals(port, that.port) && Objects.equals(hosts, that.hosts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(port, hosts);
    }
}
