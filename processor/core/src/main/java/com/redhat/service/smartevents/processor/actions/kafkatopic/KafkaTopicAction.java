package com.redhat.service.smartevents.processor.actions.kafkatopic;

import com.redhat.service.smartevents.processor.GatewayBean;

public interface KafkaTopicAction extends GatewayBean {

    String TYPE = "KafkaTopic";
    String TOPIC_PARAM = "topic";

    @Override
    default String getType() {
        return TYPE;
    }
}
