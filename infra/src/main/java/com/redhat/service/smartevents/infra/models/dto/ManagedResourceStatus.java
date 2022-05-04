package com.redhat.service.smartevents.infra.models.dto;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ManagedResourceStatus {
    // Creation flow: accepted -> preparing -> provisioning -> ready
    ACCEPTED("accepted"),
    PREPARING("preparing"),
    PROVISIONING("provisioning"),
    READY("ready"),
    // Deleting flow: ready -> deprovision -> deleting -> deleted
    DEPROVISION("deprovision"),
    DELETING("deleting"),
    DELETED("deleted"),
    // When something has gone wrong!
    FAILED("failed");

    String status;

    // We can not annotate the property `status` directly with `@JsonValue`. See https://issues.redhat.com/browse/MGDOBR-595
    @JsonValue
    public String serialize() {
        return status;
    }

    ManagedResourceStatus(String status) {
        this.status = status;
    }
}
