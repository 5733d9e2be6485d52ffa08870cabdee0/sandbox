package com.redhat.service.smartevents.processor.actions.kafkatopic;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.validations.ValidationResult;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class KafkaTopicActionValidatorTest {

    @Inject
    KafkaTopicActionValidator validator;

    private Action createActionWithTopic(String topicName) {
        Action action = new Action();
        action.setType(KafkaTopicAction.TYPE);
        Map<String, String> params = new HashMap<>();
        params.put(KafkaTopicAction.TOPIC_PARAM, topicName);
        action.setMapParameters(params);
        return action;
    }

    @Test
    void isValid() {
        Action action = createActionWithTopic("myTopic");
        assertThat(validator.isValid(action).isValid()).isTrue();
    }

    @Test
    void isInvalid_nullParametersMapIsNotValid() {
        Action action = new Action();
        action.setType(KafkaTopicAction.TYPE);
        action.setMapParameters(new HashMap<>());
        ValidationResult validationResult = validator.isValid(action);
        assertThat(validationResult.isValid()).isFalse();
    }

    @Test
    void isInvalid_noTopicIsNotValid() {
        Action action = createActionWithTopic("myTopic");
        action.getParameters().remove(KafkaTopicAction.TOPIC_PARAM);
        ValidationResult validationResult = validator.isValid(action);

        assertThat(validationResult.isValid()).isFalse();
        assertThat(validationResult.getMessage()).isEqualTo(KafkaTopicActionValidator.INVALID_TOPIC_PARAM_MESSAGE);
    }

    @Test
    void isInvalid_emptyTopicStringIsNotValid() {
        Action action = createActionWithTopic("myTopic");
        action.getParameters().put(KafkaTopicAction.TOPIC_PARAM, "");
        ValidationResult validationResult = validator.isValid(action);

        assertThat(validationResult.isValid()).isFalse();
        assertThat(validationResult.getMessage()).isEqualTo(KafkaTopicActionValidator.INVALID_TOPIC_PARAM_MESSAGE);
    }
}
