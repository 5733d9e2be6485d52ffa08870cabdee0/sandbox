package com.redhat.service.bridge.infra.models.dto;

// TODO-MC review statuses
// https://issues.redhat.com/browse/MGDOBR-195
public enum ConnectorStatus {
    ACCEPTED("accepted"),
    TOPIC_CREATED("topic_created"),
    READY("ready"), // connector and topic have been created
    TOPIC_DELETED("topic_deleted"),
    DELETED("deleted"), // connector and topic have been created, used only as Desired status as it's physically deleted eventually
    FAILED("failed");

    String status;

    ConnectorStatus(String status) {
        this.status = status;
    }
}
