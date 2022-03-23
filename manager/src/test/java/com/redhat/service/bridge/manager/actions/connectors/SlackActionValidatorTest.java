package com.redhat.service.bridge.manager.actions.connectors;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.validations.ValidationResult;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class SlackActionValidatorTest {

    @Inject
    SlackActionValidator validator;

    @Test
    void isInvalidWithEmptyChannelParameter() {
        Map<String, String> params = new HashMap<>();
        params.put(SlackAction.CHANNEL_PARAMETER, "");
        params.put(SlackAction.WEBHOOK_URL_PARAMETER, "w");
        assertIsInvalid(actionWith(params), SlackActionValidator.INVALID_CHANNEL_MESSAGE);
    }

    @Test
    void isInvalidWithEmptyWebhookURLParameter() {
        Map<String, String> params = new HashMap<>();
        params.put(SlackAction.CHANNEL_PARAMETER, "c");
        params.put(SlackAction.WEBHOOK_URL_PARAMETER, "");
        assertIsInvalid(actionWith(params), SlackActionValidator.INVALID_WEBHOOK_URL_MESSAGE);
    }

    @Test
    void isValidWithBothParameters() {
        Map<String, String> params = new HashMap<>();
        params.put(SlackAction.CHANNEL_PARAMETER, "channel");
        params.put(SlackAction.WEBHOOK_URL_PARAMETER, "webhook");
        assertIsValid(actionWith(params));
    }

    private void assertIsValid(BaseAction action) {
        ValidationResult validationResult = validator.isValid(action);
        assertThat(validationResult.isValid()).isTrue();
    }

    private void assertIsInvalid(BaseAction action, String errorMessage) {
        ValidationResult validationResult = validator.isValid(action);
        assertThat(validationResult.isValid()).isFalse();
        if (errorMessage == null) {
            assertThat(validationResult.getMessage()).isNull();
        } else {
            assertThat(validationResult.getMessage()).startsWith(errorMessage);
        }
    }

    private BaseAction actionWith(Map<String, String> params) {
        BaseAction b = new BaseAction();
        b.setType(SlackAction.TYPE);
        b.setParameters(params);
        return b;
    }
}
