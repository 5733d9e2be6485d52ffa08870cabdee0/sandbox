package com.redhat.service.smartevents.processor.actions.sendtobridge;

import com.redhat.service.smartevents.processor.GatewayBean;

public interface SendToBridgeAction extends GatewayBean {

    String TYPE = "send_to_bridge_sink_0.1";
    String BRIDGE_ID_PARAM = "bridgeId";

    @Override
    default String getType() {
        return TYPE;
    }
}
