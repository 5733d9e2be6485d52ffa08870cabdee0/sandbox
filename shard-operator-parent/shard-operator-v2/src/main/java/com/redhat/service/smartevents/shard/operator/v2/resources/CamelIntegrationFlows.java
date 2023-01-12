package com.redhat.service.smartevents.shard.operator.v2.resources;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

public class CamelIntegrationFlows {
    List<JsonNode> froms = new ArrayList<>();

    public List<JsonNode> getFroms() {
        return froms;
    }

    public void setFroms(List<JsonNode> froms) {
        this.froms = froms;
    }

    @Override
    public String toString() {
        return "CamelIntegrationFlows{" +
                "froms=" + froms +
                '}';
    }
}
