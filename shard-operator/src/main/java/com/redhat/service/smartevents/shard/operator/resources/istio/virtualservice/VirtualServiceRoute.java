package com.redhat.service.smartevents.shard.operator.resources.istio.virtualservice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VirtualServiceRoute {

    private VirtualServiceDestination destination;

    public VirtualServiceDestination getDestination() {
        return destination;
    }

    public void setDestination(VirtualServiceDestination destination) {
        this.destination = destination;
    }
}
