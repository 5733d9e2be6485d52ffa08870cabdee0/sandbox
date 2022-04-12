package com.redhat.service.smartevents.processor.actions.kafkatopic;

import com.redhat.service.smartevents.processor.actions.ActionBean;

public interface KafkaTopicAction extends ActionBean {

    String TYPE = "KafkaTopic";
    String TOPIC_PARAM = "topic";

    @Override
    default String getType() {
        return TYPE;
    }
}
