package com.redhat.service.bridge.infra.models.actions;

import java.util.Map;

public class ActionFactory {
    public static BaseAction buildAction(String type, String name, Map<String, String> parameters) {
        switch (type) {
            case KafkaTopicAction.KAFKA_ACTION_TYPE:
                return new KafkaTopicAction(name, parameters);
            default:
                throw new IllegalArgumentException("Type " + type + " is not valid");
        }
    }
}
