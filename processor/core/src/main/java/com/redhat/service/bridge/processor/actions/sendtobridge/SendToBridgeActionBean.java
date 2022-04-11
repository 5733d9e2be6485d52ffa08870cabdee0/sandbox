package com.redhat.service.bridge.processor.actions.sendtobridge;

import com.redhat.service.bridge.processor.actions.ActionBean;

public interface SendToBridgeActionBean extends ActionBean {

    String TYPE = "SendToBridge";
    String BRIDGE_ID_PARAM = "bridgeId";

    @Override
    default String getType() {
        return TYPE;
    }
}
