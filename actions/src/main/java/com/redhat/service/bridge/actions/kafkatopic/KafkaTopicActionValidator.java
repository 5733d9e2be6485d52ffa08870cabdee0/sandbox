package com.redhat.service.bridge.actions.kafkatopic;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.bridge.actions.ActionParameterValidator;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.validations.ValidationResult;

@ApplicationScoped
public class KafkaTopicActionValidator implements ActionParameterValidator {

    public static final String INVALID_TOPIC_PARAM_MESSAGE = "The supplied topic parameter is not valid";

    @Override
    public String getType() {
        return KafkaTopicAction.TYPE;
    }

    @Override
    public ValidationResult isValid(BaseAction baseAction) {
        if (baseAction.getParameters() != null) {
            String topic = baseAction.getParameters().get(KafkaTopicAction.TOPIC_PARAM);
            if (topic == null || topic.isEmpty()) {
                return ValidationResult.invalid(INVALID_TOPIC_PARAM_MESSAGE);
            }
            return ValidationResult.valid();
        }
        return ValidationResult.invalid();
    }
}
