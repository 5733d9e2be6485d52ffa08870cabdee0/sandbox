package com.redhat.service.bridge.infra.api.models.dto;

public enum BridgeStatus {
    REQUESTED,
    PROVISIONING,
    AVAILABLE,
    DELETION_REQUESTED,
    DELETED,
    FAILED
}
