package com.redhat.service.smartevents.processor.actions;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ActionServiceMock implements ActionService {

    @Override
    public String getBridgeEndpoint(String bridgeId, String customerId) {
        return String.format("https://example.com/%s/events", bridgeId);
    }

    @Override
    public String getConnectorTopicName(String processorId) {
        return String.format("mock-%s", processorId);
    }
}
