package com.redhat.service.bridge.shard.operator;

import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;

public class TestConstants {

    public static final String CUSTOMER_ID = "myCustomer";
    public static final String INGRESS_IMAGE = "myimage:latest";
    public static final String BRIDGE_ID = "my-id";
    public static final String BRIDGE_NAME = "my-name";

    public static final BridgeDTO newRequestedBridgeDTO() {
        return new BridgeDTO(BRIDGE_ID, BRIDGE_NAME, "http://localhost:8080", CUSTOMER_ID, BridgeStatus.REQUESTED);
    }
}
