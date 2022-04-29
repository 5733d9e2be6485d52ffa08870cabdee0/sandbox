package com.redhat.service.smartevents.processor.actions.source;

import com.redhat.service.smartevents.processor.GatewayBean;

public interface SourceAction extends GatewayBean {

    String TYPE = "Source";
    String ENDPOINT_PARAM = "endpoint";
    String CLOUD_EVENT_TYPE_PARAM = "cloudEventType";

    @Override
    default String getType() {
        return TYPE;
    }
}
