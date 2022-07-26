package com.redhat.service.smartevents.processor.actions.logger;

import com.redhat.service.smartevents.processor.GatewayBean;

public interface LoggerAction  extends GatewayBean {
    String TYPE = "logger_sink_0.1";

    @Override
    default String getType() {
        return TYPE;
    }
}
