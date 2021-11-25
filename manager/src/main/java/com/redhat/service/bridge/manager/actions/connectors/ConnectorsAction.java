package com.redhat.service.bridge.manager.actions.connectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.bridge.actions.ActionProvider;
import com.redhat.service.bridge.actions.ActionTransformer;

@ApplicationScoped
public class ConnectorsAction implements ActionProvider {

    public static final String TYPE = "Connectors";
    public static final String CONNECTOR_PAYLOAD = "connectorPayload";

    @Inject
    ConnectorsActionValidator validator;

    @Inject
    ConnectorsActionTransformer transformer;

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public ConnectorsActionValidator getParameterValidator() {
        return validator;
    }

    @Override
    public ActionTransformer getTransformer() {
        return transformer;
    }
}
