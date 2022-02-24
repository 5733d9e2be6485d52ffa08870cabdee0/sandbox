package com.redhat.service.bridge.infra.models.dto;

// TODO-MC review statuses
// https://issues.redhat.com/browse/MGDOBR-195
public enum ConnectorStatus {
    ACCEPTED("accepted"),
    TOPIC_CREATED("topic_created"), // Kafka Topic created.
    MANAGED_CONNECTOR_CREATED("managed_connector_created"), // Kafka Topic and OB Connector created.
    MANAGED_CONNECTOR_LOOKUP_FAILED("managed_connector_lookup_failed"), // Kafka Topic and OB Connector created but MC Connector is not found.
    READY("ready"), // Kafka Topic, OB Connector and MC Connector have been created.
    TOPIC_DELETED("topic_deleted"),
    DELETED("deleted"), // connector and topic have been created, used only as Desired status as it's physically deleted eventually
    FAILED("failed");

    String status;

    ConnectorStatus(String status) {
        this.status = status;
    }
}
