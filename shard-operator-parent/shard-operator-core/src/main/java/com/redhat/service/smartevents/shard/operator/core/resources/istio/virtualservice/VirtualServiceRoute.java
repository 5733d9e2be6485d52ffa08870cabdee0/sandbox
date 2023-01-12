package com.redhat.service.smartevents.shard.operator.core.resources.istio.virtualservice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VirtualServiceRoute {

    private VirtualServiceDestination destination;

    public VirtualServiceDestination getDestination() {
        return destination;
    }

    public void setDestination(VirtualServiceDestination destination) {
        this.destination = destination;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VirtualServiceRoute that = (VirtualServiceRoute) o;
        return Objects.equals(destination, that.destination);
    }

    @Override
    public int hashCode() {
        return Objects.hash(destination);
    }
}
