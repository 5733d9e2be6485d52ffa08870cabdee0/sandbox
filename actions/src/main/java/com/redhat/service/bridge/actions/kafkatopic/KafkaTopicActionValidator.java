package com.redhat.service.bridge.actions.kafkatopic;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.bridge.actions.ActionParameterValidator;
import com.redhat.service.bridge.infra.models.actions.BaseAction;

@ApplicationScoped
public class KafkaTopicActionValidator implements ActionParameterValidator {

    @Override
    public boolean isValid(BaseAction baseAction) {
        if (baseAction.getParameters() != null) {
            String topic = baseAction.getParameters().get(KafkaTopicAction.TOPIC_PARAM);
            return topic != null && !topic.isEmpty();
        }
        return false;
    }
}
