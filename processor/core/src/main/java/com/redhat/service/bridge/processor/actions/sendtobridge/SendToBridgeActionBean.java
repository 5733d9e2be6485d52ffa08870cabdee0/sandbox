package com.redhat.service.bridge.processor.actions.sendtobridge;

import com.redhat.service.bridge.processor.actions.ActionBean;

public interface SendToBridgeActionBean extends ActionBean {

    @Override
    default String getType() {
        return SendToBridgeAction.TYPE;
    }
}
