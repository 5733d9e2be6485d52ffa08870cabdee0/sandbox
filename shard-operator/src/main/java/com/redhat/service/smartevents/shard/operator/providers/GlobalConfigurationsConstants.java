package com.redhat.service.smartevents.shard.operator.providers;

public class GlobalConfigurationsConstants {
    public static final String KAFKA_BOOTSTRAP_SERVERS_ENV_VAR = "KAFKA_BOOTSTRAP_SERVERS";
    public static final String KAFKA_CLIENT_ID_ENV_VAR = "KAFKA_CLIENT_ID";
    public static final String KAFKA_CLIENT_SECRET_ENV_VAR = "KAFKA_CLIENT_SECRET";
    public static final String KAFKA_SECURITY_PROTOCOL_ENV_VAR = "KAFKA_SECURITY_PROTOCOL";
    public static final String KAFKA_TOPIC_ENV_VAR = "KAFKA_TOPIC";
    public static final String KAFKA_GROUP_ID_ENV_VAR = "KAFKA_GROUP_ID";

    public static final String KNATIVE_KAFKA_PROTOCOL_SECRET = "protocol";
    public static final String KNATIVE_KAFKA_SASL_MECHANISM_SECRET = "sasl.mechanism";
    public static final String KNATIVE_KAFKA_USER_SECRET = "user";
    public static final String KNATIVE_KAFKA_PASSWORD_SECRET = "password";
    public static final String KNATIVE_KAFKA_BOOTSTRAP_SERVERS_SECRET = "bootstrap.servers";
    public static final String KNATIVE_KAFKA_TOPIC_NAME_SECRET = "topic.name";

    public static final String KNATIVE_KAFKA_TOPIC_PARTITIONS_CONFIGMAP = "default.topic.partitions";
    public static final String KNATIVE_KAFKA_REPLICATION_FACTOR_CONFIGMAP = "default.topic.replication.factor";
    public static final String KNATIVE_KAFKA_TOPIC_BOOTSTRAP_SERVERS_CONFIGMAP = "bootstrap.servers";
    public static final String KNATIVE_KAFKA_TOPIC_SECRET_REF_NAME_CONFIGMAP = "auth.secret.ref.name";
    public static final String KNATIVE_KAFKA_TOPIC_TOPIC_NAME_CONFIGMAP = "topic.name";
}
