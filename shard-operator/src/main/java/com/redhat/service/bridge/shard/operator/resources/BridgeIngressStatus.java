package com.redhat.service.bridge.shard.operator.resources;

import java.util.List;

/**
 * To be defined on <a href="MGDOBR-91">https://issues.redhat.com/browse/MGDOBR-91</a>
 * <p>
 * This status MUST implement the status best practices as defined by the
 * <a href="https://github.com/kubernetes/community/blob/master/contributors/devel/sig-architecture/api-conventions.md#typical-status-properties">Kubernetes API Conventions</a>
 */
public class BridgeIngressStatus {

    private List<String> conditions;

    private PhaseType phase;

    private String endpoint;

    public BridgeIngressStatus() {
        // Left blank on purpose
    }

    public BridgeIngressStatus(PhaseType phase) {
        this.phase = phase;
    }

    public List<String> getConditions() {
        return conditions;
    }

    public void setConditions(List<String> conditions) {
        this.conditions = conditions;
    }

    public PhaseType getPhase() {
        return phase;
    }

    public void setPhase(PhaseType phase) {
        this.phase = phase;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}
