package com.redhat.service.bridge.infra.models.dto;

// TODO-MC review statuses
// https://issues.redhat.com/browse/MGDOBR-195
public enum ConnectorStatus {
    REQUESTED,
    PROVISIONING,
    AVAILABLE,
    DELETION_REQUESTED,
    DELETED,
    FAILED
}
