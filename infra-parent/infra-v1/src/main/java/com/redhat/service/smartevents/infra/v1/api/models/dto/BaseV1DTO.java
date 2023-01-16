package com.redhat.service.smartevents.infra.v1.api.models.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.core.models.dtos.BaseDTO;
import com.redhat.service.smartevents.infra.v1.api.models.ManagedResourceStatusV1;

@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class BaseV1DTO extends BaseDTO {

    @JsonProperty("status")
    protected ManagedResourceStatusV1 status;

    public BaseV1DTO() {
    }

    public BaseV1DTO(String id,
            String name,
            String customerId,
            String owner,
            ManagedResourceStatusV1 status) {
        super(id, name, customerId, owner);
        this.status = status;
    }

    public ManagedResourceStatusV1 getStatus() {
        return status;
    }

    public void setStatus(ManagedResourceStatusV1 status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "BaseV1DTO{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", customerId='" + customerId + '\'' +
                ", owner='" + owner + '\'' +
                ", status=" + status +
                '}';
    }
}
