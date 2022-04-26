package com.redhat.service.smartevents.processor.actions.slack;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.validations.ValidationResult;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class SlackActionValidatorTest {

    @Inject
    SlackActionValidator validator;

    @Test
    void isInvalidWithNoParameters() {
        Map<String, String> params = new HashMap<>();
        assertIsInvalid(actionWith(params), SlackActionValidator.INVALID_CHANNEL_MESSAGE);
    }

    @Test
    void isInvalidWithMissingChannelParameter() {
        Map<String, String> params = new HashMap<>();
        params.put(SlackAction.WEBHOOK_URL_PARAM, "w");
        assertIsInvalid(actionWith(params), SlackActionValidator.INVALID_CHANNEL_MESSAGE);
    }

    @Test
    void isInvalidWithEmptyChannelParameter() {
        Map<String, String> params = new HashMap<>();
        params.put(SlackAction.CHANNEL_PARAM, "");
        params.put(SlackAction.WEBHOOK_URL_PARAM, "w");
        assertIsInvalid(actionWith(params), SlackActionValidator.INVALID_CHANNEL_MESSAGE);
    }

    @Test
    void isInvalidWithMissingWebhookURLParameter() {
        Map<String, String> params = new HashMap<>();
        params.put(SlackAction.CHANNEL_PARAM, "c");
        assertIsInvalid(actionWith(params), SlackActionValidator.INVALID_WEBHOOK_URL_MESSAGE);
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

    private void assertIsValid(Action action) {
        ValidationResult validationResult = validator.isValid(action);
        assertThat(validationResult.isValid()).isTrue();
    }

    private void assertIsInvalid(Action action, String errorMessage) {
        ValidationResult validationResult = validator.isValid(action);
        assertThat(validationResult.isValid()).isFalse();
        if (errorMessage == null) {
            assertThat(validationResult.getMessage()).isNull();
        } else {
            assertThat(validationResult.getMessage()).startsWith(errorMessage);
        }
    }

    private Action actionWith(Map<String, String> params) {
        Action action = new Action();
        action.setType(SlackAction.TYPE);
        action.setParameters(params);
        return action;
    }
}
