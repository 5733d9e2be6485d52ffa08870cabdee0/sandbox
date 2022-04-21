package com.redhat.service.smartevents.processor.actions.sendtobridge;

import com.redhat.service.smartevents.processor.GatewayBean;

public interface SendToBridgeAction extends GatewayBean {

    String TYPE = "SendToBridge";
    String BRIDGE_ID_PARAM = "bridgeId";

    @Override
    default String getType() {
        return TYPE;
    }
}
