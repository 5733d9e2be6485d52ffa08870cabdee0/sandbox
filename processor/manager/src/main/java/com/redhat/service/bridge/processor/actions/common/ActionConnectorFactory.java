package com.redhat.service.bridge.processor.actions.common;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ActionConnectorFactory extends AbstractActionAccepterFactory<ActionConnector> {

    public ActionConnectorFactory() {
        super(ActionConnector.class);
    }

    public boolean hasConnector(String actionType) {
        return getOptional(actionType).isPresent();
    }
}
