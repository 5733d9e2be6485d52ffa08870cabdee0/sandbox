package com.redhat.service.bridge.processor.actions.slack;

import com.redhat.service.bridge.processor.actions.common.ActionAccepter;

public interface SlackAction extends ActionAccepter {

    String TYPE = "Slack";

    String CHANNEL_PARAMETER = "channel";
    String WEBHOOK_URL_PARAMETER = "webhookUrl";

    @Override
    default String getType() {
        return TYPE;
    }
}
