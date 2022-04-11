package com.redhat.service.bridge.processor.actions.slack;

import com.redhat.service.bridge.processor.actions.ActionBean;

public interface SlackActionBean extends ActionBean {

    String TYPE = "Slack";
    String CHANNEL_PARAM = "channel";
    String WEBHOOK_URL_PARAM = "webhookUrl";

    @Override
    default String getType() {
        return TYPE;
    }
}
