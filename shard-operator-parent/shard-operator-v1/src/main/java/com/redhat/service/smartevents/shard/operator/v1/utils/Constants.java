package com.redhat.service.smartevents.shard.operator.v1.utils;

public class Constants {
    private Constants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String CUSTOMER_ID_CONFIG_ENV_VAR = "EVENT_BRIDGE_CUSTOMER_ID";
    public static final String EVENT_BRIDGE_LOGGING_JSON = "EVENT_BRIDGE_LOGGING_JSON";
    public static final String BRIDGE_INGRESS_WEBHOOK_TECHNICAL_ACCOUNT_ID = "EVENT_BRIDGE_WEBHOOK_TECHNICAL_ACCOUNT_ID";
    public static final String BRIDGE_EXECUTOR_PROCESSOR_DEFINITION_ENV_VAR = "PROCESSOR_DEFINITION";
    public static final String BRIDGE_EXECUTOR_WEBHOOK_SSO_ENV_VAR = "SSO_SERVER_URL";
    public static final String BRIDGE_EXECUTOR_WEBHOOK_CLIENT_ID_ENV_VAR = "WEBHOOK_CLIENT_ID";
    public static final String BRIDGE_EXECUTOR_WEBHOOK_CLIENT_SECRET_ENV_VAR = "WEBHOOK_CLIENT_SECRET";

    public static final String BRIDGE_INGRESS_AUTHORIZATION_POLICY_SELECTOR_LABEL = "istio";
}