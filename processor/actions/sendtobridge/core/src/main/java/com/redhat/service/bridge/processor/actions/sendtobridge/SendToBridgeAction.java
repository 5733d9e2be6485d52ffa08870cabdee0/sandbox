package com.redhat.service.bridge.processor.actions.sendtobridge;

import com.redhat.service.bridge.processor.actions.common.ActionAccepter;

public interface SendToBridgeAction extends ActionAccepter {

    String TYPE = "SendToBridge";
    String BRIDGE_ID_PARAM = "bridgeId";

    @Override
    default String getType() {
        return TYPE;
    }
}
