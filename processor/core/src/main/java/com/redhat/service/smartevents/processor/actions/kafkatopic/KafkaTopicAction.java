package com.redhat.service.smartevents.processor.actions.kafkatopic;

import com.redhat.service.smartevents.infra.processor.actions.KafkaTopicConstants;
import com.redhat.service.smartevents.processor.GatewayBean;

public interface KafkaTopicAction extends GatewayBean {

    String TYPE = "kafka_topic_sink_0.1";
    String TOPIC_PARAM = KafkaTopicConstants.TOPIC_PARAM;
    String BROKER_URL = KafkaTopicConstants.KAFKA_BROKER_URL;
    String CLIENT_ID = KafkaTopicConstants.KAFKA_CLIENT_ID;
    String CLIENT_SECRET = KafkaTopicConstants.KAFKA_CLIENT_SECRET;
    String SECURITY_PROTOCOL = KafkaTopicConstants.KAFKA_SECURITY_PROTOCOL;
    String BRIDGE_ERROR_TOPIC_NAME = KafkaTopicConstants.BRIDGE_ERROR_TOPIC_NAME;

    @Override
    default String getType() {
        return TYPE;
    }
}
