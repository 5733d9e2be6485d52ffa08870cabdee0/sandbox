package com.redhat.service.smartevents.processor.actions.http;

import com.redhat.service.smartevents.processor.GatewayBean;

public interface HttpAction extends GatewayBean {

    String TYPE = "http_sink_0.1";
    String HTTP_URL = "http_url";
    String HTTP_METHOD = "http_method";

    @Override
    default String getType() {
        return TYPE;
    }
}
