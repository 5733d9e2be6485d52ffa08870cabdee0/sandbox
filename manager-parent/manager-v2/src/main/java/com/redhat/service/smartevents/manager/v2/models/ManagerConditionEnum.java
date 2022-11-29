package com.redhat.service.smartevents.manager.v2.models;

public enum ManagerConditionEnum {
    KAFKA_TOPIC_READY("KafkaTopicReady"),
    KAFKA_TOPIC_PERMISSIONS_READY("KafkaTopicPermissionsReady"),
    DNS_RECORD_READY("DnsRecordReady"),
    DATA_PLANE_READY("DataPlaneReady");

    private final String value;

    ManagerConditionEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return this.getValue();
    }
}
