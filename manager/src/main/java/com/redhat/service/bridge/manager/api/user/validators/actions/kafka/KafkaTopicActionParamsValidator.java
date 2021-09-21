package com.redhat.service.bridge.manager.api.user.validators.actions.kafka;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.actions.KafkaTopicAction;
import com.redhat.service.bridge.manager.api.user.validators.actions.ActionParamsValidator;

@ApplicationScoped
public class KafkaTopicActionParamsValidator implements ActionParamsValidator {

    @Override
    public boolean accepts(BaseAction baseAction) {
        return KafkaTopicAction.KAFKA_ACTION_TYPE.equals(baseAction.getType());
    }

    @Override
    public boolean isValid(BaseAction baseAction) {
        if (baseAction.getParameters() != null) {
            String topic = baseAction.getParameters().get(KafkaTopicAction.KAFKA_ACTION_TOPIC_PARAM);
            return topic != null && !topic.isEmpty();
        }
        return false;
    }
}
