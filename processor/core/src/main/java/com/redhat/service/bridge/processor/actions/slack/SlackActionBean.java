package com.redhat.service.bridge.processor.actions.slack;

import com.redhat.service.bridge.processor.actions.ActionBean;

public interface SlackActionBean extends ActionBean {

    String TYPE = "Slack";

    String CHANNEL_PARAMETER = "channel";
    String WEBHOOK_URL_PARAMETER = "webhookUrl";

    @Override
    default String getType() {
        return TYPE;
    }
}
