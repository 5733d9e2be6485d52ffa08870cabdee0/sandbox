package com.redhat.service.smartevents.shard.operator.core.resources.istio.gateway;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GatewaySpecSelector {

    private String istio;

    public String getIstio() {
        return istio;
    }

    public void setIstio(String istio) {
        this.istio = istio;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GatewaySpecSelector that = (GatewaySpecSelector) o;
        return Objects.equals(istio, that.istio);
    }

    @Override
    public int hashCode() {
        return Objects.hash(istio);
    }
}
