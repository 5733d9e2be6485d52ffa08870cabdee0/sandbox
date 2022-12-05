package com.redhat.service.smartevents.infra.v2.api.models;

public final class DefaultConditions {

    public static final String CP_CONTROL_PLANE_READY_NAME = "ControlPlaneReady";
    public static final String CP_DATA_PLANE_READY_NAME = "DataPlaneReady";
    public static final String CP_KAFKA_TOPIC_READY_NAME = "KafkaTopicReady";
    public static final String CP_DNS_RECORD_READY_NAME = "DnsRecordReady";

    public static final String DP_SECRET_READY_NAME = "SecretReady";
    public static final String DP_IMAGE_READY_NAME = "ImageReady";
    public static final String DP_DEPLOYMENT_READY_NAME = "DeploymentReady";
    public static final String DP_SERVICE_READY_NAME = "ServiceReady";
    public static final String DP_SERVICE_MONITOR_READY_NAME = "ServiceMonitorReady";
}
