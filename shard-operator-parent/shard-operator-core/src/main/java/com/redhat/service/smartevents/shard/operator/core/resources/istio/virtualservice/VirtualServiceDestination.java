package com.redhat.service.smartevents.shard.operator.core.resources.istio.virtualservice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VirtualServiceDestination that = (VirtualServiceDestination) o;
        return Objects.equals(host, that.host) && Objects.equals(port, that.port);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }
}
