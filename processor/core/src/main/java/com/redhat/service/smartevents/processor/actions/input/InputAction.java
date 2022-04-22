package com.redhat.service.smartevents.processor.actions.input;

import com.redhat.service.smartevents.processor.GatewayBean;

public interface InputAction extends GatewayBean {

    String TYPE = "Input";
    String ENDPOINT_PARAM = "endpoint";
    String CLOUD_EVENT_TYPE = "cloudEventType";

    @Override
    default String getType() {
        return TYPE;
    }
}
