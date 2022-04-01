package com.redhat.service.bridge.processor.actions.kafkatopic;

import java.util.concurrent.TimeUnit;

import com.redhat.service.bridge.processor.actions.common.ActionAccepter;

public interface KafkaTopicAction extends ActionAccepter {

    String TYPE = "KafkaTopic";

    String TOPIC_PARAM = "topic";

    long DEFAULT_LIST_TOPICS_TIMEOUT = 10L;

    TimeUnit DEFAULT_LIST_TOPICS_TIMEUNIT = TimeUnit.SECONDS;

    @Override
    default String getType() {
        return TYPE;
    }
}
