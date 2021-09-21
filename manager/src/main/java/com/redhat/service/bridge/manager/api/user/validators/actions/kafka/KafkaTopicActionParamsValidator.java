package com.redhat.service.bridge.manager.api.user.validators.actions.kafka;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.bridge.infra.models.actions.ActionRequest;
import com.redhat.service.bridge.infra.models.actions.KafkaTopicAction;
import com.redhat.service.bridge.manager.api.user.validators.actions.ActionParamsValidator;

@ApplicationScoped
public class KafkaTopicActionParamsValidator implements ActionParamsValidator {

    @Override
    public boolean accepts(ActionRequest actionRequest) {
        return KafkaTopicAction.KAFKA_ACTION_TYPE.equals(actionRequest.getType());
    }

    @Override
    public boolean isValid(ActionRequest actionRequest) {
        if (actionRequest.getParameters() != null) {
            String topic = actionRequest.getParameters().get(KafkaTopicAction.KAFKA_ACTION_TOPIC_PARAM);
            return topic != null && !topic.isEmpty();
        }
        return false;
    }
}
