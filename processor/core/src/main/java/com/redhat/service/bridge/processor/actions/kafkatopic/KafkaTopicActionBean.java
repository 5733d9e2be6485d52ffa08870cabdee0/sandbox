package com.redhat.service.bridge.processor.actions.kafkatopic;

import com.redhat.service.bridge.processor.actions.ActionBean;

public interface KafkaTopicActionBean extends ActionBean {

    @Override
    default String getType() {
        return KafkaTopicAction.TYPE;
    }
}
