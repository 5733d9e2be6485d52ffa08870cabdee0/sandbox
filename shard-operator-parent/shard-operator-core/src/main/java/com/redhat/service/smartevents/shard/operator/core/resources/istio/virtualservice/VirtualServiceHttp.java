package com.redhat.service.smartevents.shard.operator.core.resources.istio.virtualservice;

import java.util.List;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        VirtualServiceHttp that = (VirtualServiceHttp) o;
        return Objects.equals(match, that.match) && Objects.equals(route, that.route);
    }

    @Override
    public int hashCode() {
        return Objects.hash(match, route);
    }
}
