package com.redhat.service.smartevents.processor.sources.slack;

import com.redhat.service.smartevents.processor.GatewayBean;

public interface SlackSource extends GatewayBean {

    String TYPE = "Slack";
    String CHANNEL_PARAM = "channel";
    String TOKEN_PARAM = "token";

    @Override
    default String getType() {
        return TYPE;
    }
}
