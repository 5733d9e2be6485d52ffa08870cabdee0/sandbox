package com.redhat.service.bridge.processor.actions.sendtobridge;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.bridge.actions.ActionProvider;

@ApplicationScoped
public class SendToBridgeAction implements ActionProvider {

    public static final String TYPE = "SendToBridge";
    public static final String BRIDGE_ID_PARAM = "bridgeId";

    @Override
    public String getType() {
        return TYPE;
    }
}
