package com.redhat.service.smartevents.processor.actions.slack;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.processor.GatewayBean;

public interface SlackAction extends GatewayBean<Action> {

    String TYPE = "Slack";
    String CHANNEL_PARAM = "channel";
    String WEBHOOK_URL_PARAM = "webhookUrl";

    @Override
    default String getType() {
        return TYPE;
    }
}
