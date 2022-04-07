package com.redhat.service.bridge.processor.actions;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ActionResolverTestMock implements ActionResolverService {

    @Override
    public String getBridgeEndpoint(String bridgeId, String customerId) {
        return String.format("https://example.com/%s/events", bridgeId);
    }

    @Override
    public String getProcessorTopicName(String processorId) {
        return String.format("mock-%s", processorId);
    }
}
