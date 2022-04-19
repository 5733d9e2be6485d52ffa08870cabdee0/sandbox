package com.redhat.service.smartevents.infra.models.dto;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ManagedResourceStatus {
    ACCEPTED("accepted"),
    PROVISIONING("provisioning"),
    READY("ready"),
    DEPROVISION("deprovision"),
    DELETING("deleting"),
    DELETED("deleted"),
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