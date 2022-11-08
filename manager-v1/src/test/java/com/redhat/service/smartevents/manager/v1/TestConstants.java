package com.redhat.service.smartevents.manager.v1;

import com.redhat.service.smartevents.infra.v1.api.models.processors.ProcessorType;

public class TestConstants {

    public static final String DEFAULT_BRIDGE_ID = "myId";
    public static final String DEFAULT_CUSTOMER_ID = "kekkobar";
    public static final String DEFAULT_ORGANISATION_ID = "myOrg";
    public static final String DEFAULT_USER_NAME = "kekkobar";
    public static final String DEFAULT_BRIDGE_NAME = "myBridge";
    public static final String DEFAULT_BRIDGE_ENDPOINT = "https://bridge.redhat.com";
    public static final String DEFAULT_BRIDGE_TLS_CERTIFICATE = "tlsCertificate";
    public static final String DEFAULT_BRIDGE_TLS_KEY = "tlsKey";
    public static final String DEFAULT_ERROR_HANDLER_PROCESSOR_NAME = "error-handler-processor-name";

    public static final String SHARD_ID = DEFAULT_CUSTOMER_ID;
    public static final String DEFAULT_SHARD_ROUTER_CANONICAL_HOSTNAME = "router." + SHARD_ID + ".com";

    public static final ProcessorType DEFAULT_PROCESSOR_TYPE = ProcessorType.SINK;
    public static final String DEFAULT_PROCESSOR_ID = "myProcessorId";
    public static final String DEFAULT_PROCESSOR_NAME = "processMcProcessor";

    public static final String DEFAULT_KAFKA_TOPIC = "myTestTopic";
    public static final String DEFAULT_CLIENT_ID = "myClientId";
    public static final String DEFAULT_CLIENT_SECRET = "myClientSecret";

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_PAGE_SIZE = 10;

    public static final String DEFAULT_CONNECTOR_NAME = "myConnector";

    public static final String DEFAULT_CLOUD_PROVIDER = "aws";
    public static final String DEFAULT_REGION = "us-east-1";
}
