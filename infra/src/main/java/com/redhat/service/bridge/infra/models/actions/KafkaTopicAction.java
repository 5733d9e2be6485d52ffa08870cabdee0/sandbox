package com.redhat.service.bridge.infra.models.actions;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KafkaTopicAction extends BaseAction {

    public static final String KAFKA_ACTION_TYPE = "KafkaTopicAction";

    public static final String KAFKA_ACTION_TOPIC_PARAM = "topic";

    @JsonProperty(value = BaseAction.ACTION_TYPE_FIELD)
    private String type = KAFKA_ACTION_TYPE;

    @NotEmpty(message = "topic parameter must be supplied for KafkaTopicAction")
    @JsonProperty(value = KAFKA_ACTION_TOPIC_PARAM)
    private String topic;

    public KafkaTopicAction() {
    }

    public KafkaTopicAction(String name, String topic) {
        super(name);
        this.topic = topic;
    }

    public KafkaTopicAction(String name, Map<String, String> parameters) {
        super(name, parameters);
    }

    public String getType() {
        return type;
    }

    @Override
    public Map<String, String> getParameters() {
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put(KAFKA_ACTION_TOPIC_PARAM, topic);
        return parameters;
    }

    @Override
    public void setParameters(Map<String, String> parameters) {
        if (!parameters.containsKey(KAFKA_ACTION_TOPIC_PARAM)) {
            throw new IllegalArgumentException(String.format("KafkaTopicAction must include the parameter '%s'", KAFKA_ACTION_TOPIC_PARAM));
        }
        this.topic = parameters.get(KAFKA_ACTION_TOPIC_PARAM);
    }
}
