package com.redhat.service.bridge.manager.actions.sendtobridge;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.bridge.actions.ActionProvider;
import com.redhat.service.bridge.actions.ActionTransformer;

@ApplicationScoped
public class SendToBridgeAction implements ActionProvider {

    public static final String TYPE = "SendToBridge";
    public static final String BRIDGE_ID_PARAM = "bridgeId";

    @Inject
    SendToBridgeActionValidator validator;

    @Inject
    SendToBridgeActionTransformer transformer;

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public SendToBridgeActionValidator getParameterValidator() {
        return validator;
    }

    @Override
    public ActionTransformer getTransformer() {
        return transformer;
    }
}
