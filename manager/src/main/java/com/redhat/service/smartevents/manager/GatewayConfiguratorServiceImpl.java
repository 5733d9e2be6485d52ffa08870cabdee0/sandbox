package com.redhat.service.smartevents.manager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.manager.providers.InternalKafkaConfigurationProvider;
import com.redhat.service.smartevents.manager.providers.ResourceNamesProvider;
import com.redhat.service.smartevents.processor.GatewayConfiguratorService;

@ApplicationScoped
public class GatewayConfiguratorServiceImpl implements GatewayConfiguratorService {

    @Inject
    BridgesService bridgesService;

    @Inject
    ResourceNamesProvider resourceNamesProvider;

    @Inject
    InternalKafkaConfigurationProvider internalKafkaConfigurationProvider;

    @Override
    public String getBridgeEndpoint(String bridgeId, String customerId) {
        return bridgesService.getReadyBridge(bridgeId, customerId).getEndpoint();
    }

    @Override
    public String getConnectorTopicName(String processorId, String actionName) {
        return resourceNamesProvider.getProcessorTopicName(processorId, actionName);
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
