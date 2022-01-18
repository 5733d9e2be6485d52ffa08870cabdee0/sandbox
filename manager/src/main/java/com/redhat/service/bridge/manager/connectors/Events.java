package com.redhat.service.bridge.manager.connectors;

public interface Events {

    String CONNECTOR_CREATED_EVENT = "connector-created";
    String KAFKA_TOPIC_CREATED_EVENT = "kafka-topic-created";

    String CONNECTOR_DELETED_EVENT = "connector-deleted";
    String KAFKA_TOPIC_DELETED_EVENT = "kakfa-topic-deleted";
}
