package com.redhat.service.smartevents.manager.core.mocks;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.processor.GatewayConfiguratorService;

@ApplicationScoped
public class GatewayConfiguratorServiceForTests implements GatewayConfiguratorService {

    @Override
    public String getBridgeEndpoint(String bridgeId, String customerId) {
        return null;
    }

    @Override
    public String getConnectorTopicName(String processorId) {
        return null;
    }

    @Override
    public String getBootstrapServers() {
        return null;
    }

    @Override
    public String getClientId() {
        return null;
    }

    @Override
    public String getClientSecret() {
        return null;
    }

    @Override
    public String getSecurityProtocol() {
        return null;
    }
}
