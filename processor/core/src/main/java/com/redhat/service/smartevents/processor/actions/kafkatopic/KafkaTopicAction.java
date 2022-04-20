package com.redhat.service.smartevents.processor.actions.kafkatopic;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.processor.GatewayBean;

public interface KafkaTopicAction extends GatewayBean<Action> {

    String TYPE = "KafkaTopic";
    String TOPIC_PARAM = "topic";

    @Override
    default String getType() {
        return TYPE;
    }
}
