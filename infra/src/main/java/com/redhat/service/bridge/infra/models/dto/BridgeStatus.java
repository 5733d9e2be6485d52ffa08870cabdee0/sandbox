package com.redhat.service.bridge.infra.models.dto;

public enum BridgeStatus {
    REQUESTED,
    PROVISIONING,
    AVAILABLE,
    DELETION_REQUESTED,
    DELETING,
    DELETED,
    FAILED
}
