package com.redhat.service.smartevents.infra.models.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateManagedResourceStatusDTO {

    private String id;

    private String customerId;

    private ManagedResourceStatus status;

    public UpdateManagedResourceStatusDTO() {
    }

    public UpdateManagedResourceStatusDTO(String id, String customerId, ManagedResourceStatus status) {
        this.id = id;
        this.customerId = customerId;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public ManagedResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ManagedResourceStatus status) {
        this.status = status;
    }
}
