package com.redhat.service.smartevents.processor.actions.input;

import com.redhat.service.smartevents.processor.actions.ActionBean;

public interface InputAction extends ActionBean {

    String TYPE = "Input";
    String ENDPOINT_PARAM = "endpoint";
    String CLOUD_EVENT_TYPE = "cloudEventType";

    @Override
    default String getType() {
        return TYPE;
    }
}
