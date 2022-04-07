package com.redhat.service.bridge.processor.actions.slack;

import com.redhat.service.bridge.processor.actions.ActionBean;

public interface SlackActionBean extends ActionBean {

    @Override
    default String getType() {
        return SlackAction.TYPE;
    }
}
