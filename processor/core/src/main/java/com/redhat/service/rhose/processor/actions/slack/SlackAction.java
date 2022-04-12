package com.redhat.service.rhose.processor.actions.slack;

import com.redhat.service.rhose.processor.actions.ActionBean;

public interface SlackAction extends ActionBean {

    String TYPE = "Slack";
    String CHANNEL_PARAM = "channel";
    String WEBHOOK_URL_PARAM = "webhookUrl";

    @Override
    default String getType() {
        return TYPE;
    }
}
