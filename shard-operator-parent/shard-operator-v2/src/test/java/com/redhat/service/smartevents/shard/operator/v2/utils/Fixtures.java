package com.redhat.service.smartevents.shard.operator.v2.utils;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.redhat.service.smartevents.infra.core.api.dto.KafkaConnectionDTO;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.DNSConfigurationDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.KnativeBrokerConfigurationDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.SourceConfigurationDTO;
import com.redhat.service.smartevents.shard.operator.v2.TestSupport;
import com.redhat.service.smartevents.shard.operator.v2.converters.ManagedBridgeConverter;
import com.redhat.service.smartevents.shard.operator.v2.converters.ManagedProcessorConverter;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridge;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedProcessor;

public class Fixtures {

    public static final String CUSTOMER_ID = "my-customer";
    public static final String USER_NAME = "my-user-name";
    public static final String BRIDGE_ID = "my-id";
    public static final String BRIDGE_NAME = "my-name";
    public static final String BRIDGE_ENDPOINT = "http://localhost:8080";
    public static final String BRIDGE_HOST = "localhost";
    public static final String BRIDGE_TLS_CERTIFICATE = "tlsCertificate";
    public static final String BRIDGE_TLS_KEY = "tlsKey";
    public static final String KAFKA_BOOTSTRAP_SERVERS = "mytestkafka:9092";
    public static final String KAFKA_CLIENT_ID = "client-id";
    public static final String KAFKA_CLIENT_SECRET = "testsecret";
    public static final String KAFKA_SECURITY_PROTOCOL = "PLAINTEXT";
    public static final String KAFKA_SASL_MECHANISM = "PLAIN";
    public static final String KAFKA_TOPIC = "ob-my-id";
    public static final String KAFKA_ERROR_TOPIC = "ob-my-id-errors";
    public static final String PROCESSOR_ID = "my-processor-id";
    public static final String PROCESSOR_NAME = "my-processor-name";
    public static final KafkaConnectionDTO KAFKA_CONNECTION_DTO = new KafkaConnectionDTO(
            KAFKA_BOOTSTRAP_SERVERS,
            KAFKA_CLIENT_ID,
            KAFKA_CLIENT_SECRET,
            KAFKA_SECURITY_PROTOCOL,
            KAFKA_SASL_MECHANISM,
            KAFKA_TOPIC,
            KAFKA_ERROR_TOPIC);

    public static BridgeDTO createBridge(OperationType operation) {
        return new BridgeDTO(BRIDGE_ID,
                BRIDGE_NAME,
                CUSTOMER_ID,
                USER_NAME,
                new DNSConfigurationDTO(BRIDGE_ENDPOINT, BRIDGE_TLS_CERTIFICATE, BRIDGE_TLS_KEY),
                new KnativeBrokerConfigurationDTO(KAFKA_CONNECTION_DTO),
                new SourceConfigurationDTO(KAFKA_CONNECTION_DTO),
                operation,
                TestSupport.RESOURCE_TIMEOUT);
    }

    public static ManagedBridge createManagedBridge(BridgeDTO bridgeDTO, String namespace) {
        return ManagedBridgeConverter.fromBridgeDTOToManageBridge(bridgeDTO, namespace);
    }

    public static ProcessorDTO createProcessor(OperationType operation) {
        return new ProcessorDTO(PROCESSOR_ID,
                PROCESSOR_NAME,
                JsonNodeFactory.instance.objectNode(),
                BRIDGE_ID,
                CUSTOMER_ID,
                USER_NAME,
                operation,
                TestSupport.RESOURCE_TIMEOUT);
    }

    public static ManagedProcessor createManagedProcessor(ProcessorDTO processorDTO, String namespace) {
        return ManagedProcessorConverter.fromProcessorDTOToManagedProcessor(processorDTO, namespace);
    }
}
