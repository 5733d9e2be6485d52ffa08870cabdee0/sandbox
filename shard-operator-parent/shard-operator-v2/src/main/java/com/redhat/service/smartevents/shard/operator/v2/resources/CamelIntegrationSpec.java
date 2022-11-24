package com.redhat.service.smartevents.shard.operator.v2.resources;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

public class CamelIntegrationSpec {
    List<JsonNode> flows;

    public List<JsonNode> getFlows() {
        return flows;
    }

    public void setFlows(List<JsonNode> flows) {
        this.flows = flows;
    }

    @Override
    public String toString() {
        return "CamelIntegrationSpec{" +
                "flows=" + flows +
                '}';
    }
}
