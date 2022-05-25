package com.redhat.service.smartevents.processor.sources.errorhandler;

import com.redhat.service.smartevents.processor.GatewayBean;

public interface ErrorHandlerSource extends GatewayBean {

    String TYPE = "ErrorHandler";

    @Override
    default String getType() {
        return TYPE;
    }
}
