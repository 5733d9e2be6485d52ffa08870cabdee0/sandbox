package com.redhat.service.bridge.manager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.bridge.manager.providers.ResourceNamesProvider;
import com.redhat.service.bridge.processor.actions.ActionResolverService;

@ApplicationScoped
public class ActionResolverServiceImpl implements ActionResolverService {

    @Inject
    BridgesService bridgesService;

    @Inject
    ResourceNamesProvider resourceNamesProvider;

    @Override
    public String getBridgeEndpoint(String bridgeId, String customerId) {
        return bridgesService.getReadyBridge(bridgeId, customerId).getEndpoint();
    }

    @Override
    public String getProcessorTopicName(String processorId) {
        return resourceNamesProvider.getProcessorTopicName(processorId);
    }
}
