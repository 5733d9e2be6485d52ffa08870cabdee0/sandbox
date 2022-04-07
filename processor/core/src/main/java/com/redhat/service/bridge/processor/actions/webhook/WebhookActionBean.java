package com.redhat.service.bridge.processor.actions.webhook;

import com.redhat.service.bridge.processor.actions.ActionBean;

public interface WebhookActionBean extends ActionBean {

    @Override
    default String getType() {
        return WebhookAction.TYPE;
    }
}
