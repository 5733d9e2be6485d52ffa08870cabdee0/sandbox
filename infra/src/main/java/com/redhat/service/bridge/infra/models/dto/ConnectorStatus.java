package com.redhat.service.bridge.infra.models.dto;

// TODO-MC review statuses
public enum ConnectorStatus {
    REQUESTED,
    PROVISIONING,
    AVAILABLE,
    DELETION_REQUESTED,
    DELETED,
    FAILED
}
