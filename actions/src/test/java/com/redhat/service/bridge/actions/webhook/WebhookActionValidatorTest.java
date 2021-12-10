package com.redhat.service.bridge.actions.webhook;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.actions.ValidationResult;
import com.redhat.service.bridge.infra.api.models.actions.BaseAction;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class WebhookActionValidatorTest {

    @Inject
    WebhookActionValidator validator;

    @Test
    void isValidWithHttpProtocol() {
        assertIsValid("http://www.example.com/webhook");
    }

    @Test
    void isValidWithHttpsProtocol() {
        assertIsValid("https://www.example.com/webhook");
    }

    @Test
    void isInvalidWithNullParameter() {
        assertIsInvalid(null, WebhookActionValidator.MISSING_ENDPOINT_PARAM_MESSAGE);
    }

    @Test
    void isInvalidWithEmptyParameter() {
        assertIsInvalid("", WebhookActionValidator.MISSING_ENDPOINT_PARAM_MESSAGE);
    }

    @Test
    void isInvalidWithNonUrlEndpoint() {
        assertIsInvalid("this-is-not-a-valid-url", WebhookActionValidator.MALFORMED_ENDPOINT_PARAM_MESSAGE);
    }

    @Test
    void isInvalidWithIncompleteEndpoint() {
        assertIsInvalid("www.example.com/webhook", WebhookActionValidator.MALFORMED_ENDPOINT_PARAM_MESSAGE);
    }

    @Test
    void isInvalidWithUnknownProtocol() {
        assertIsInvalid("pizza://www.example.com/webhook", WebhookActionValidator.MALFORMED_ENDPOINT_PARAM_MESSAGE);
    }

    @Test
    void isInvalidWithWrongProtocol() {
        assertIsInvalid("ftp://www.example.com/webhook", WebhookActionValidator.INVALID_PROTOCOL_MESSAGE);
    }

    private void assertIsValid(String endpoint) {
        BaseAction action = createActionWithEndpoint(endpoint);
        ValidationResult validationResult = validator.isValid(action);
        assertThat(validationResult.isValid()).isTrue();
    }

    private void assertIsInvalid(String endpoint, String errorMessage) {
        BaseAction action = createActionWithEndpoint(endpoint);
        ValidationResult validationResult = validator.isValid(action);
        assertThat(validationResult.isValid()).isFalse();
        assertThat(validationResult.getMessage()).startsWith(errorMessage);
    }

    private BaseAction createActionWithEndpoint(String endpoint) {
        BaseAction b = new BaseAction();
        b.setType(WebhookAction.TYPE);
        Map<String, String> params = new HashMap<>();
        params.put(WebhookAction.ENDPOINT_PARAM, endpoint);
        b.setParameters(params);
        return b;
    }
}
