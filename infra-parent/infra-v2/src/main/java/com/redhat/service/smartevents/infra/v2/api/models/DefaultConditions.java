package com.redhat.service.smartevents.infra.v2.api.models;

public final class DefaultConditions {

    public static final String CP_CONTROL_PLANE_READY_NAME = "ControlPlaneReady";
    public static final String CP_DATA_PLANE_READY_NAME = "DataPlaneReady";
    public static final String CP_KAFKA_TOPIC_READY_NAME = "KafkaTopicReady";
    public static final String CP_DNS_RECORD_READY_NAME = "DnsRecordReady";

    public static final String CP_CONTROL_PLANE_DELETED_NAME = "ControlPlaneDeleted";
    public static final String CP_DATA_PLANE_DELETED_NAME = "DataPlaneDeleted";
    public static final String CP_KAFKA_TOPIC_DELETED_NAME = "KafkaTopicDeleted";
    public static final String CP_DNS_RECORD_DELETED_NAME = "DnsRecordDeleted";

    public static final String DP_SECRET_READY_NAME = "SecretReady";
    public static final String DP_CONFIG_MAP_READY_NAME = "ConfigMapReady";
    public static final String DP_KNATIVE_BROKER_READY_NAME = "KNativeBrokerReady";
    public static final String DP_AUTHORISATION_POLICY_READY_NAME = "AuthorisationPolicyReady";
    public static final String DP_NETWORK_RESOURCE_READY_NAME = "NetworkResourceReady";

    public static final String DP_BRIDGE_DELETED_NAME = "BridgeDeleted";
}
