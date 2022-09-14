package com.redhat.service.smartevents.shard.operator.resources.istio.virtualservice;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VirtualServiceHttp {

    private List<VirtualServiceMatch> match;

    private List<VirtualServiceRoute> route;

    public List<VirtualServiceMatch> getMatch() {
        return match;
    }

    public void setMatch(List<VirtualServiceMatch> match) {
        this.match = match;
    }

    public List<VirtualServiceRoute> getRoute() {
        return route;
    }

    public void setRoute(List<VirtualServiceRoute> route) {
        this.route = route;
    }
}
