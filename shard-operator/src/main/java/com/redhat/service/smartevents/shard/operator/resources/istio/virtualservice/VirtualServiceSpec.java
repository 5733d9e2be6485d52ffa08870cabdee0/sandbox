package com.redhat.service.smartevents.shard.operator.resources.istio.virtualservice;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
}
