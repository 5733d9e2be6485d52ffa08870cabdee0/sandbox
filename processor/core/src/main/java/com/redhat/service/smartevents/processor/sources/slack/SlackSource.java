package com.redhat.service.smartevents.processor.sources.slack;

import com.redhat.service.smartevents.processor.GatewayBean;

public interface SlackSource extends GatewayBean {

    String TYPE = "slack_source_0.1";
    String CHANNEL_PARAM = "slack_channel";
    String TOKEN_PARAM = "slack_token";

    @Override
    default String getType() {
        return TYPE;
    }
}
