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

    @JsonValue
    final String status;

    ManagedResourceStatus(String status) {
        this.status = status;
    }
}