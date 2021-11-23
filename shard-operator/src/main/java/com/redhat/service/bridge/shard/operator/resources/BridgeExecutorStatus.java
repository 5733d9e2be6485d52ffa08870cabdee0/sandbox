package com.redhat.service.bridge.shard.operator.resources;

import java.util.List;

public class BridgeExecutorStatus {

    private List<String> conditions;

    private PhaseType phase;

    public BridgeExecutorStatus() {
        // Left blank on purpose
    }

    public BridgeExecutorStatus(PhaseType phase) {
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
}
