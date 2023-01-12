package com.redhat.service.smartevents.shard.operator.core.resources.istio.virtualservice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VirtualServiceSpec {

    private List<String> gateways;

    private List<String> hosts;

    private List<VirtualServiceHttp> http;

    public List<String> getGateways() {
        return gateways;
    }

    public void setGateways(List<String> gateways) {
        this.gateways = gateways;
    }

    public List<String> getHosts() {
        return hosts;
    }

    public void setHosts(List<String> hosts) {
        this.hosts = hosts;
    }

    public List<VirtualServiceHttp> getHttp() {
        return http;
    }

    public void setHttp(List<VirtualServiceHttp> http) {
        this.http = http;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VirtualServiceSpec that = (VirtualServiceSpec) o;
        return Objects.equals(gateways, that.gateways) && Objects.equals(hosts, that.hosts) && Objects.equals(http, that.http);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gateways, hosts, http);
    }
}
