package com.redhat.service.bridge.infra.models.dto;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ManagedEntityStatus {
    ACCEPTED("accepted"),
    PREPARING("preparing"),
    PROVISIONING("provisioning"),
    READY("ready"),
    DEPROVISION("deprovision"),
    DELETING("deleting"),
    DELETED("deleted"),
    FAILED("failed");

    @JsonValue
    String status;

    ManagedEntityStatus(String status) {
        this.status = status;
    }
}