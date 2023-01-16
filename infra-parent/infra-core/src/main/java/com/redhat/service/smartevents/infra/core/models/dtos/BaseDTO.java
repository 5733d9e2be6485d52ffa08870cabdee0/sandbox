package com.redhat.service.smartevents.infra.core.models.dtos;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.core.metrics.SupportsMetrics;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class BaseDTO implements SupportsMetrics {
    @JsonProperty("id")
    protected String id;

    @JsonProperty("name")
    protected String name;

    @JsonProperty("customerId")
    protected String customerId;

    @JsonProperty("owner")
    protected String owner;

    protected BaseDTO() {
    }

    protected BaseDTO(String id, String name, String customerId, String owner) {
        this.id = id;
        this.name = name;
        this.customerId = customerId;
        this.owner = owner;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BaseDTO dto = (BaseDTO) o;
        return id.equals(dto.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "BaseDTO{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", customerId='" + customerId + '\'' +
                ", owner='" + owner + '\'' +
                '}';
    }
}
