package com.redhat.service.smartevents.processor.actions.slack;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.models.actions.BaseAction;
import com.redhat.service.smartevents.infra.validations.ValidationResult;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class SlackActionValidatorTest {

    @Inject
    SlackActionValidator validator;

    @Test
    void isInvalidWithEmptyChannelParameter() {
        Map<String, String> params = new HashMap<>();
        params.put(SlackAction.CHANNEL_PARAM, "");
        params.put(SlackAction.WEBHOOK_URL_PARAM, "w");
        assertIsInvalid(actionWith(params), SlackActionValidator.INVALID_CHANNEL_MESSAGE);
    }

    @Test
    void isInvalidWithEmptyWebhookURLParameter() {
        Map<String, String> params = new HashMap<>();
        params.put(SlackAction.CHANNEL_PARAM, "c");
        params.put(SlackAction.WEBHOOK_URL_PARAM, "");
        assertIsInvalid(actionWith(params), SlackActionValidator.INVALID_WEBHOOK_URL_MESSAGE);
    }

    @Test
    void isValidWithBothParameters() {
        Map<String, String> params = new HashMap<>();
        params.put(SlackAction.CHANNEL_PARAM, "channel");
        params.put(SlackAction.WEBHOOK_URL_PARAM, "webhook");
        assertIsValid(actionWith(params));
    }

    private void assertIsValid(BaseAction action) {
        ValidationResult validationResult = validator.isValid(action);
        Assertions.assertThat(validationResult.isValid()).isTrue();
    }

    private void assertIsInvalid(BaseAction action, String errorMessage) {
        ValidationResult validationResult = validator.isValid(action);
        Assertions.assertThat(validationResult.isValid()).isFalse();
        if (errorMessage == null) {
            Assertions.assertThat(validationResult.getMessage()).isNull();
        } else {
            Assertions.assertThat(validationResult.getMessage()).startsWith(errorMessage);
        }
    }

    private BaseAction actionWith(Map<String, String> params) {
        BaseAction b = new BaseAction();
        b.setType(SlackAction.TYPE);
        b.setParameters(params);
        return b;
    }
}
