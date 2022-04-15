package com.redhat.service.smartevents.processor.actions.input;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.processor.GatewayBean;

public interface InputAction extends GatewayBean<Action> {

    String TYPE = "Input";
    String ENDPOINT_PARAM = "endpoint";
    String CLOUD_EVENT_TYPE = "cloudEventType";

    @Override
    default String getType() {
        return TYPE;
    }
}
