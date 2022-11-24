package com.redhat.service.smartevents.shard.operator.v2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.redhat.service.smartevents.infra.core.api.dto.KafkaConnectionDTO;
import com.redhat.service.smartevents.infra.v1.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.v1.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.v1.api.models.filters.BaseFilter;
import com.redhat.service.smartevents.infra.v1.api.models.filters.StringEquals;
import com.redhat.service.smartevents.infra.v1.api.models.gateways.Action;
import com.redhat.service.smartevents.infra.v1.api.models.processors.ProcessorDefinition;
import com.redhat.service.smartevents.infra.v1.api.models.processors.ProcessorType;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;

import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.PREPARING;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.PROVISIONING;
import static com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus.READY;

public class TestSupport {

    public static final String CUSTOMER_ID = "myCustomer";
    public static final String USER_NAME = "myUserName";
    public static final String EXECUTOR_IMAGE = "openbridge/executor:latest";
    public static final String BRIDGE_ID = "my-id";
    public static final String BRIDGE_NAME = "my-name";
    public static final String BRIDGE_ENDPOINT = "http://localhost:8080";
    public static final String BRIDGE_HOST = "localhost";
    public static final String BRIDGE_TLS_CERTIFICATE = "tlsCertificate";
    public static final String BRIDGE_TLS_KEY = "tlsKey";
    public static final ProcessorType PROCESSOR_TYPE = ProcessorType.SINK;
    public static final String PROCESSOR_ID = "my-processor-id";
    public static final String PROCESSOR_NAME = "my-processor-name";
    public static final String KAFKA_BOOTSTRAP_SERVERS = "mytestkafka:9092";
    public static final String KAFKA_CLIENT_ID = "client-id";
    public static final String KAFKA_CLIENT_SECRET = "testsecret";
    public static final String KAFKA_SECURITY_PROTOCOL = "PLAINTEXT";
    public static final String KAFKA_SASL_MECHANISM = "PLAIN";
    public static final String KAFKA_TOPIC = "ob-my-id";
    public static final String KAFKA_ERROR_TOPIC = "ob-my-id-errors";

    public static final KafkaConnectionDTO KAFKA_CONNECTION_DTO = new KafkaConnectionDTO(
            KAFKA_BOOTSTRAP_SERVERS,
            KAFKA_CLIENT_ID,
            KAFKA_CLIENT_SECRET,
            KAFKA_SECURITY_PROTOCOL,
            KAFKA_SASL_MECHANISM,
            KAFKA_TOPIC,
            KAFKA_ERROR_TOPIC);

    public static BridgeDTO newRequestedBridgeDTO() {
        return new BridgeDTO(BRIDGE_ID, BRIDGE_NAME, BRIDGE_ENDPOINT, BRIDGE_TLS_CERTIFICATE, BRIDGE_TLS_KEY, CUSTOMER_ID, USER_NAME, PREPARING, KAFKA_CONNECTION_DTO);
    }

    public static BridgeDTO newProvisioningBridgeDTO() {
        return new BridgeDTO(BRIDGE_ID, BRIDGE_NAME, BRIDGE_ENDPOINT, BRIDGE_TLS_CERTIFICATE, BRIDGE_TLS_KEY, CUSTOMER_ID, USER_NAME, PROVISIONING, KAFKA_CONNECTION_DTO);
    }

    public static BridgeDTO newAvailableBridgeDTO() {
        return new BridgeDTO(BRIDGE_ID, BRIDGE_NAME, BRIDGE_ENDPOINT, BRIDGE_TLS_CERTIFICATE, BRIDGE_TLS_KEY, CUSTOMER_ID, USER_NAME, READY, KAFKA_CONNECTION_DTO);
    }

    public static ProcessorDTO newRequestedProcessorDTO() {
        Set<BaseFilter> filters = new HashSet<>();
        filters.add(new StringEquals("key", "value"));

        String transformationTemplate = "{\"test\": {key}}";

        Action a = new Action();
        a.setType(KafkaTopicAction.TYPE);

        Map<String, String> params = new HashMap<>();
        params.put(KafkaTopicAction.TOPIC_PARAM, "myTopic");
        a.setMapParameters(params);

        ProcessorDefinition definition = new ProcessorDefinition(filters, transformationTemplate, a);

        ProcessorDTO dto = new ProcessorDTO();
        dto.setType(PROCESSOR_TYPE);
        dto.setId(PROCESSOR_ID);
        dto.setName(PROCESSOR_NAME);
        dto.setDefinition(definition);
        dto.setBridgeId(BRIDGE_ID);
        dto.setCustomerId(CUSTOMER_ID);
        dto.setOwner(USER_NAME);
        dto.setStatus(PREPARING);
        dto.setKafkaConnection(KAFKA_CONNECTION_DTO);
        return dto;
    }
}
