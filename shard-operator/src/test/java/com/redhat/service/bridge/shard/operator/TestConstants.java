package com.redhat.service.bridge.shard.operator;

import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;

public class TestConstants {

    public static final String CUSTOMER_ID = "myCustomer";
    public static final String INGRESS_IMAGE = "myimage:latest";
    public static final String EXECUTOR_IMAGE = "myimage:latest";
    public static final String BRIDGE_ID = "my-id";
    public static final String BRIDGE_NAME = "my-name";
    public static final String BRIDGE_ENDPOINT = "http://localhost:8080";
    public static final String PROCESSOR_ID = "my-processor-id";
    public static final String PROCESSOR_NAME = "my-processor-name";

    public static BridgeDTO newRequestedBridgeDTO() {
        return new BridgeDTO(BRIDGE_ID, BRIDGE_NAME, BRIDGE_ENDPOINT, CUSTOMER_ID, BridgeStatus.REQUESTED);
    }

    public static BridgeDTO newProvisioningBridgeDTO() {
        return new BridgeDTO(BRIDGE_ID, BRIDGE_NAME, BRIDGE_ENDPOINT, CUSTOMER_ID, BridgeStatus.PROVISIONING);
    }

    public static BridgeDTO newAvailableBridgeDTO() {
        return new BridgeDTO(BRIDGE_ID, BRIDGE_NAME, BRIDGE_ENDPOINT, CUSTOMER_ID, BridgeStatus.AVAILABLE);
    }
}
