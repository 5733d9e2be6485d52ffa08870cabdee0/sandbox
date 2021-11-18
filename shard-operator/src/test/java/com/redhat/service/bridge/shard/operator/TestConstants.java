package com.redhat.service.bridge.shard.operator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.infra.models.filters.BaseFilter;
import com.redhat.service.bridge.infra.models.filters.StringEquals;
import com.redhat.service.bridge.infra.models.processors.ProcessorDefinition;

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

    public static ProcessorDTO newRequestedProcessorDTO() {
        BridgeDTO bridgeDTO = newAvailableBridgeDTO();

        Set<BaseFilter> filters = new HashSet<>();
        filters.add(new StringEquals("key", "value"));

        String transformationTemplate = "{\"test\": {key}}";

        BaseAction a = new BaseAction();
        a.setType(KafkaTopicAction.TYPE);
        a.setName("kafkaAction");

        Map<String, String> params = new HashMap<>();
        params.put(KafkaTopicAction.TOPIC_PARAM, "myTopic");
        a.setParameters(params);

        ProcessorDefinition definition = new ProcessorDefinition(filters, transformationTemplate, a);

        return new ProcessorDTO(PROCESSOR_ID, PROCESSOR_NAME, definition, bridgeDTO, BridgeStatus.REQUESTED);
    }
}
