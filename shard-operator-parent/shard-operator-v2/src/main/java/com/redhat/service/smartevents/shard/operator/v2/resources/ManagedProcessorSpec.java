package com.redhat.service.smartevents.shard.operator.v2.resources;

import java.util.Objects;

public class ManagedProcessorSpec {
    private String id;

    private String name;

    private String bridgeId;

    private String shardId;

    private String flows;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBridgeId() {
        return bridgeId;
    }

    public void setBridgeId(String bridgeId) {
        this.bridgeId = bridgeId;
    }

    public String getShardId() {
        return shardId;
    }

    public void setShardId(String shardId) {
        this.shardId = shardId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFlows() {
        return flows;
    }

    public void setFlows(String flows) {
        this.flows = flows;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ManagedProcessorSpec that = (ManagedProcessorSpec) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(bridgeId, that.bridgeId) && Objects.equals(shardId, that.shardId)
                && Objects.equals(flows, that.flows);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, bridgeId, shardId, flows);
    }
}
