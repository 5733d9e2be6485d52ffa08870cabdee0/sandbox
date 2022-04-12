package com.redhat.service.smartevents.processor.actions.sendtobridge;

import com.redhat.service.smartevents.processor.actions.ActionBean;

public interface SendToBridgeAction extends ActionBean {

    String TYPE = "SendToBridge";
    String BRIDGE_ID_PARAM = "bridgeId";

    @Override
    default String getType() {
        return TYPE;
    }
}
