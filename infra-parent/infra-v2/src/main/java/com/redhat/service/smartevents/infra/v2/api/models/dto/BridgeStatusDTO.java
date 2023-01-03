package com.redhat.service.smartevents.infra.v2.api.models.dto;

import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BridgeStatusDTO {

    @JsonProperty("id")
    private String id;

    @JsonProperty("generation")
    private long generation;

    @JsonProperty("conditions")
    private Set<ConditionDTO> conditions;

    public BridgeStatusDTO() {
    }

    public BridgeStatusDTO(String id, long generation, Set<ConditionDTO> conditions) {
        this.id = id;
        this.generation = generation;
        this.conditions = conditions;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getGeneration() {
        return generation;
    }

    public void setGeneration(long generation) {
        this.generation = generation;
    }

    public Set<ConditionDTO> getConditions() {
        return conditions;
    }

    public void setConditions(Set<ConditionDTO> conditions) {
        this.conditions = conditions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BridgeStatusDTO that = (BridgeStatusDTO) o;
        return generation == that.generation && Objects.equals(id, that.id) && Objects.equals(conditions, that.conditions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, generation, conditions);
    }
}
