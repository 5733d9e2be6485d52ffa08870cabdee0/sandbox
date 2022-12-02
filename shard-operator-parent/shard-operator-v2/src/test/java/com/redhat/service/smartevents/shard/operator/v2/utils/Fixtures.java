package com.redhat.service.smartevents.shard.operator.v2.utils;

import com.redhat.service.smartevents.infra.core.api.dto.KafkaConnectionDTO;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedBridge;

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

    public static final KafkaConnectionDTO KAFKA_CONNECTION_DTO = new KafkaConnectionDTO(
            KAFKA_BOOTSTRAP_SERVERS,
            KAFKA_CLIENT_ID,
            KAFKA_CLIENT_SECRET,
            KAFKA_SECURITY_PROTOCOL,
            KAFKA_SASL_MECHANISM,
            KAFKA_TOPIC,
            KAFKA_ERROR_TOPIC);

    public static BridgeDTO createBridge(OperationType operation) {
        return new BridgeDTO(BRIDGE_ID, BRIDGE_NAME, BRIDGE_ENDPOINT, BRIDGE_TLS_CERTIFICATE, BRIDGE_TLS_KEY, CUSTOMER_ID, USER_NAME, KAFKA_CONNECTION_DTO, operation);
    }

    public static ManagedBridge createManagedBridge(BridgeDTO bridgeDTO, String namespace) {
        return ManagedBridge.fromDTO(bridgeDTO, namespace);
    }
}
