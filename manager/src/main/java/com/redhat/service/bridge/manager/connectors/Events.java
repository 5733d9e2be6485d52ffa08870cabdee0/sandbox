package com.redhat.service.bridge.manager.connectors;

public interface Events {

    String CONNECTOR_ACCEPTED_EVENT = "connector-requested";
    String CONNECTOR_KAFKA_TOPIC_CREATED_EVENT = "connector-kafka-topic-created";
    String CONNECTOR_MANAGED_CONNECTOR_CREATED_EVENT = "connector-managed-connector-created";

    String CONNECTOR_DELETED_EVENT = "connector-deleted";
    String KAFKA_TOPIC_DELETED_EVENT = "kakfa-topic-deleted";
}
