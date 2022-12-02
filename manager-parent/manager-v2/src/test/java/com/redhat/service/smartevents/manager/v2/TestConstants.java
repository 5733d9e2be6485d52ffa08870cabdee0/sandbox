package com.redhat.service.smartevents.manager.v2;

public class TestConstants {

    public static final String DEFAULT_BRIDGE_ID = "myId";
    public static final String DEFAULT_CUSTOMER_ID = "kekkobar";
    public static final String DEFAULT_ORGANISATION_ID = "myOrg";
    public static final String DEFAULT_USER_NAME = "kekkobar";
    public static final String DEFAULT_BRIDGE_NAME = "myBridge";

    public static final String SHARD_ID = DEFAULT_CUSTOMER_ID;
    public static final String DEFAULT_SHARD_ROUTER_CANONICAL_HOSTNAME = "router." + SHARD_ID + ".com";

    public static final String DEFAULT_PROCESSOR_ID = "myProcessorId";
    public static final String DEFAULT_PROCESSOR_NAME = "myProcessor";

    public static final String DEFAULT_CLOUD_PROVIDER = "aws";
    public static final String DEFAULT_REGION = "us-east-1";

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_PAGE_SIZE = 10;

    public static final String FAILED_CONDITION_ERROR_CODE = "OPENBRIDGE-1";
    public static final String FAILED_CONDITION_ERROR_MESSAGE = "Something went wrong";
}
