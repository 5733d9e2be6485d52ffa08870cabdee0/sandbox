package com.redhat.service.smartevents.processor.actions.slack;

import com.redhat.service.smartevents.processor.GatewayBean;

public interface SlackAction extends GatewayBean {

    String TYPE = "slack_sink_0.1";
    String CHANNEL_PARAM = "slack_channel";
    String WEBHOOK_URL_PARAM = "slack_webhook_url";

    @Override
    default String getType() {
        return TYPE;
    }
}
