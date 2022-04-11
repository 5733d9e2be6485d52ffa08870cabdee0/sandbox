package com.redhat.service.bridge.processor.actions.kafkatopic;

import com.redhat.service.bridge.processor.actions.ActionBean;

public interface KafkaTopicAction extends ActionBean {

    String TYPE = "KafkaTopic";
    String TOPIC_PARAM = "topic";

    @Override
    default String getType() {
        return TYPE;
    }
}
