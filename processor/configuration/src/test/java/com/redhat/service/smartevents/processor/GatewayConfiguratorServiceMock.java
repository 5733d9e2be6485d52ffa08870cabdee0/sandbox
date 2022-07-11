package com.redhat.service.smartevents.processor;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GatewayConfiguratorServiceMock implements GatewayConfiguratorService {

    @Override
    public String getBridgeEndpoint(String bridgeId, String customerId) {
        return String.format("https://example.com/%s/events", bridgeId);
    }

    @Override
    public String getConnectorTopicName(String processorId, String name) {
        return String.format("mock-%s", processorId);
    }

    @Override
    public String getBootstrapServers() {
        return "bootstrapserver:443";
    }

    @Override
    public String getClientId() {
        return "clientId";
    }

    @Override
    public String getClientSecret() {
        return "clientSecret";
    }

    @Override
    public String getSecurityProtocol() {
        return "securityProtocol";
    }
}
