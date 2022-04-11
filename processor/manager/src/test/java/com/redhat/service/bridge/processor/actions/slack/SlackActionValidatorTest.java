package com.redhat.service.bridge.processor.actions.slack;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.validations.ValidationResult;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class SlackActionValidatorTest {

    @Inject
    SlackActionValidator validator;

    @Test
    void isInvalidWithEmptyChannelParameter() {
        Map<String, String> params = new HashMap<>();
        params.put(SlackActionBean.CHANNEL_PARAM, "");
        params.put(SlackActionBean.WEBHOOK_URL_PARAM, "w");
        assertIsInvalid(actionWith(params), SlackActionValidator.INVALID_CHANNEL_MESSAGE);
    }

    @Test
    void isInvalidWithEmptyWebhookURLParameter() {
        Map<String, String> params = new HashMap<>();
        params.put(SlackActionBean.CHANNEL_PARAM, "c");
        params.put(SlackActionBean.WEBHOOK_URL_PARAM, "");
        assertIsInvalid(actionWith(params), SlackActionValidator.INVALID_WEBHOOK_URL_MESSAGE);
    }

    @Test
    void isValidWithBothParameters() {
        Map<String, String> params = new HashMap<>();
        params.put(SlackActionBean.CHANNEL_PARAM, "channel");
        params.put(SlackActionBean.WEBHOOK_URL_PARAM, "webhook");
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
        b.setType(SlackActionBean.TYPE);
        b.setParameters(params);
        return b;
    }
}
