package com.redhat.service.smartevents.integration.tests.resources.kafka;

import com.redhat.service.smartevents.integration.tests.common.Utils;

public class KafkaConnectionParameters {

    private static final String AUTH_SERVER_URL_PROPERTY = "managed.kafka.sso.auth-server-url";
    private static final String KAFKA_BOOTSTRAP_SERVERS_PROPERTY = "kafka.bootstrap.servers";
    private static final String KAFKA_ADMIN_CLIENT_ID_PROPERTY = "kafka.admin.client-id";
    private static final String KAFKA_ADMIN_CLIENT_SECRET_PROPERTY = "kafka.admin.client-secret";
    private static final String KAFKA_OPS_CLIENT_ID_PROPERTY = "kafka.ops.client.id";
    private static final String KAFKA_OPS_CLIENT_SECRET_PROPERTY = "kafka.ops.client.secret";

    private static KafkaConnectionParameters parameters;

    private String authServerURL;
    private String kafkaBootstrapServer;
    private String managedKafkaRestURL;
    private String adminClientID;
    private String adminClientSecret;
    private String opsClientID;
    private String opsClientSecret;

    private KafkaConnectionParameters() {
    }

    public static KafkaConnectionParameters getInstance() {
        if (parameters == null) {
            KafkaConnectionParameters instance = new KafkaConnectionParameters();
            instance.authServerURL = Utils.getSystemProperty(AUTH_SERVER_URL_PROPERTY);
            instance.kafkaBootstrapServer = Utils.getSystemProperty(KAFKA_BOOTSTRAP_SERVERS_PROPERTY);
            instance.managedKafkaRestURL = "https://admin-server-" + instance.kafkaBootstrapServer + "/rest";
            instance.adminClientID = Utils.getSystemProperty(KAFKA_ADMIN_CLIENT_ID_PROPERTY);
            instance.adminClientSecret = Utils.getSystemProperty(KAFKA_ADMIN_CLIENT_SECRET_PROPERTY);
            instance.opsClientID = Utils.getSystemProperty(KAFKA_OPS_CLIENT_ID_PROPERTY);
            instance.opsClientSecret = Utils.getSystemProperty(KAFKA_OPS_CLIENT_SECRET_PROPERTY);

            parameters = instance;
        }

        return parameters;
    }

    public String getAuthServerURL() {
        return authServerURL;
    }

    public String getKafkaBootstrapServer() {
        return kafkaBootstrapServer;
    }

    public String getManagedKafkaRestURL() {
        return managedKafkaRestURL;
    }

    public String getAdminClientID() {
        return adminClientID;
    }

    public String getAdminClientSecret() {
        return adminClientSecret;
    }

    public String getOpsClientID() {
        return opsClientID;
    }

    public String getOpsClientSecret() {
        return opsClientSecret;
    }
}
