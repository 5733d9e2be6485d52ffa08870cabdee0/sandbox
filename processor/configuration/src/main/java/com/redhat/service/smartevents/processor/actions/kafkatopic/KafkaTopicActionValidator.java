package com.redhat.service.smartevents.processor.actions.kafkatopic;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.infra.models.actions.Action;
import com.redhat.service.smartevents.infra.validations.ValidationResult;
import com.redhat.service.smartevents.processor.actions.ActionValidator;

@ApplicationScoped
public class KafkaTopicActionValidator implements KafkaTopicAction,
        ActionValidator {

    public static final String INVALID_TOPIC_PARAM_MESSAGE = "The supplied topic parameter is not valid";

    @Override
    public ValidationResult isValid(Action action) {
        if (action.getParameters() != null) {
            String topic = action.getParameters().get(TOPIC_PARAM);
            if (topic == null || topic.isEmpty()) {
                return ValidationResult.invalid(INVALID_TOPIC_PARAM_MESSAGE);
            }
            return ValidationResult.valid();
        }
        return ValidationResult.invalid();
    }
}
