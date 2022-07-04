package com.redhat.service.smartevents.infra.models.processors;

import java.util.Objects;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Processing {

    @JsonProperty("type")
    String type;

    @JsonProperty("spec")
    // ObjectNode is not rendered properly by swagger
    @Schema(implementation = Object.class, required = true)
    private ObjectNode spec;

    public Processing() {
    }

    public Processing(String type, ObjectNode spec) {
        this.type = type;
        this.spec = spec;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ObjectNode getSpec() {
        return spec;
    }

    public void setSpec(ObjectNode spec) {
        this.spec = spec;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Processing that = (Processing) o;
        return Objects.equals(type, that.type) && Objects.equals(spec, that.spec);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, spec);
    }

    @Override
    public String toString() {
        return "Processing{" +
                "type='" + type + '\'' +
                ", spec=" + spec +
                '}';
    }
}
