package com.redhat.service.smartevents.manager;

import com.redhat.service.smartevents.infra.models.processors.ProcessorType;

public class TestConstants {
    public static final String DEFAULT_BRIDGE_ID = "myId";
    public static final String DEFAULT_CUSTOMER_ID = "kekkobar";
    public static final String DEFAULT_ORGANISATION_ID = "myOrg";
    public static final String DEFAULT_USER_NAME = "kekkobar";
    public static final String DEFAULT_BRIDGE_NAME = "myBridge";

    public static final String SHARD_ID = DEFAULT_CUSTOMER_ID;

    public static final ProcessorType DEFAULT_PROCESSOR_TYPE = ProcessorType.SINK;
    public static final String DEFAULT_PROCESSOR_ID = "myProcessorId";
    public static final String DEFAULT_PROCESSOR_NAME = "processMcProcessor";

    public static final String DEFAULT_KAFKA_TOPIC = "myTestTopic";

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_PAGE_SIZE = 10;

    public static final String DEFAULT_CONNECTOR_NAME = "myConnector";
}
