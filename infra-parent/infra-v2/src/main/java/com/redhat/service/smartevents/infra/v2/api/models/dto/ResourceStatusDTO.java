package com.redhat.service.smartevents.infra.v2.api.models.dto;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourceStatusDTO {

    @JsonProperty("id")
    private String id;

    @JsonProperty("generation")
    private long generation;

    @JsonProperty("conditions")
    private List<ConditionDTO> conditions;

    public ResourceStatusDTO() {
    }

    public ResourceStatusDTO(String id, long generation, List<ConditionDTO> conditions) {
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

    public List<ConditionDTO> getConditions() {
        return conditions;
    }

    public void setConditions(List<ConditionDTO> conditions) {
        this.conditions = conditions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ResourceStatusDTO that = (ResourceStatusDTO) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}