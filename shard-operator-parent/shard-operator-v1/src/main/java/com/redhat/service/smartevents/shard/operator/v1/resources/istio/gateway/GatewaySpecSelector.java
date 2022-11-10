package com.redhat.service.smartevents.shard.operator.v1.resources.istio.gateway;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GatewaySpecSelector {

    private String istio;

    public String getIstio() {
        return istio;
    }

    public void setIstio(String istio) {
        this.istio = istio;
    }
}
