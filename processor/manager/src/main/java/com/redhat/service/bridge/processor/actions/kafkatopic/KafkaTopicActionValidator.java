package com.redhat.service.bridge.processor.actions.kafkatopic;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.validations.ValidationResult;
import com.redhat.service.bridge.processor.actions.ActionParameterValidator;

@ApplicationScoped
public class KafkaTopicActionValidator implements KafkaTopicActionBean, ActionParameterValidator {

    public static final String INVALID_TOPIC_PARAM_MESSAGE = "The supplied topic parameter is not valid";

    @Override
    public ValidationResult isValid(BaseAction baseAction) {
        if (baseAction.getParameters() != null) {
            String topic = baseAction.getParameters().get(KafkaTopicActionBean.TOPIC_PARAM);
            if (topic == null || topic.isEmpty()) {
                return ValidationResult.invalid(INVALID_TOPIC_PARAM_MESSAGE);
            }
            return ValidationResult.valid();
        }
        return ValidationResult.invalid();
    }
}
