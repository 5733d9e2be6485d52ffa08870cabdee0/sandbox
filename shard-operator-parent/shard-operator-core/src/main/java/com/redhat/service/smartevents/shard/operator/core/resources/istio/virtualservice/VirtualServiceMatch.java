package com.redhat.service.smartevents.shard.operator.core.resources.istio.virtualservice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VirtualServiceMatch {

    private VirtualServiceURI uri;

    public VirtualServiceURI getUri() {
        return uri;
    }

    public void setUri(VirtualServiceURI uri) {
        this.uri = uri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VirtualServiceMatch that = (VirtualServiceMatch) o;
        return Objects.equals(uri, that.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri);
    }
}
