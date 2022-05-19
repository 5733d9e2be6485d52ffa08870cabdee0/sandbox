package com.redhat.service.smartevents.processor.actions.generic;

import java.util.Map;

import javax.inject.Inject;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.validations.ValidationResult;
import com.redhat.service.smartevents.processor.actions.generic.GenericValidationConnectorJsonSchema;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static com.redhat.service.smartevents.processor.actions.generic.GenericActionConnectorTest.mapToJsonObject;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class GenericValidationConnectorJsonSchemaTest {

    @Inject
    GenericValidationConnectorJsonSchema validator;

    @Test
    void isValid() {
        Map<String, String> parametersMap = Map.of("slack_channel", "test-channel",
                                                   "slack_webhook_url", "test-webhook-url");

        Action action = actionWith(parametersMap);
        assertIsValid(action);
    }

    @Test
    void isMissingChannel() {
        Map<String, String> parametersMap = Map.of("slack_webhook_url", "test-webhook-url");

        Action action = actionWith(parametersMap);
        assertIsInvalid(action, "$.slack_channel: is missing but it is required");
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
            assertThat(validationResult.getMessage()).isEqualTo(errorMessage);
        }
    }

    private Action actionWith(Map<String, String> params) {
        Action action = new Action();
        action.setType("Generic");
        action.setParameters(params);
        action.setRawParameters(mapToJsonObject(params));
        return action;
    }
}
