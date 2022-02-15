package com.redhat.service.bridge.infra.models.dto;

public enum BridgeStatus {
    ACCEPTED("accepted"),
    PROVISIONING("provisioning"),
    READY("ready"),
    DEPROVISION("deprovision"),
    DELETING("deleting"),
    DELETED("deleted"),
    FAILED("failed");

    private String status;

    BridgeStatus(String status) {
        this.status = status;
    }
}