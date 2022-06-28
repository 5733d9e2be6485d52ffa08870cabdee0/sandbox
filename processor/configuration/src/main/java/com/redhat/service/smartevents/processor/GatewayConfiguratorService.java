package com.redhat.service.smartevents.processor;

/**
 * This interface groups the logic that this module requires from
 * the manager without requiring an explicit dependency in the pom
 * (which would cause a circular dependency).
 */
public interface GatewayConfiguratorService {

    String getBridgeEndpoint(String bridgeId, String customerId);

    String getConnectorTopicName(String processorId);

    String getBootstrapServers();

    String getClientId();

    String getClientSecret();

    String getSecurityProtocol();
}
