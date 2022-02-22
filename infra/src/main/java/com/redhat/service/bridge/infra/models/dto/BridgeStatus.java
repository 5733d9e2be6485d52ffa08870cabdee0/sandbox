package com.redhat.service.bridge.infra.models.dto;

import com.fasterxml.jackson.annotation.JsonValue;

public enum BridgeStatus {
    ACCEPTED("accepted"),
    PROVISIONING("provisioning"),
    READY("ready"),
    DEPROVISION("deprovision"),
    DELETING("deleting"),
    DELETED("deleted"),
    FAILED("failed");

    @JsonValue
    String status;

    BridgeStatus(String status) {
        this.status = status;
    }
}