package com.redhat.service.smartevents.manager.v1.services;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.manager.core.providers.InternalKafkaConfigurationProvider;
import com.redhat.service.smartevents.manager.v1.providers.ResourceNamesProviderV1;
import com.redhat.service.smartevents.processor.GatewayConfiguratorService;

@ApplicationScoped
public class GatewayConfiguratorServiceImpl implements GatewayConfiguratorService {

    @Inject
    BridgesService bridgesService;

    @Inject
    ResourceNamesProviderV1 resourceNamesProviderV1;

    @Inject
    InternalKafkaConfigurationProvider internalKafkaConfigurationProvider;

    @Override
    public String getBridgeEndpoint(String bridgeId, String customerId) {
        return bridgesService.getReadyBridge(bridgeId, customerId).getEndpoint();
    }

    @Override
    public String getConnectorTopicName(String processorId) {
        return resourceNamesProviderV1.getProcessorTopicName(processorId);
    }

    @Override
    public String getBootstrapServers() {
        return internalKafkaConfigurationProvider.getBootstrapServers();
    }

    @Override
    public String getClientId() {
        return internalKafkaConfigurationProvider.getClientId();
    }

    @Override
    public String getClientSecret() {
        return internalKafkaConfigurationProvider.getClientSecret();
    }

    @Override
    public String getSecurityProtocol() {
        return internalKafkaConfigurationProvider.getSecurityProtocol();
    }
}
