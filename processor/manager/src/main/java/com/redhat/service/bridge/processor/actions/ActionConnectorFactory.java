package com.redhat.service.bridge.processor.actions;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ActionConnectorFactory extends AbstractActionBeanFactory<ActionConnector> {

    public ActionConnectorFactory() {
        super(ActionConnector.class);
    }

    public boolean hasConnector(String actionType) {
        return getOptional(actionType).isPresent();
    }
}
