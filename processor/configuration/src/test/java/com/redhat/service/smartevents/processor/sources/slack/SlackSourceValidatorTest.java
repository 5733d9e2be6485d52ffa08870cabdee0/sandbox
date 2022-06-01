package com.redhat.service.smartevents.processor.sources.slack;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.infra.validations.ValidationResult;
import com.redhat.service.smartevents.processor.AbstractGatewayValidator;
import com.redhat.service.smartevents.processor.sources.AbstractSourceTest;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class SlackSourceValidatorTest extends AbstractSourceTest<Source> {

    @Inject
    SlackSourceValidator validator;

    @Override
    protected AbstractGatewayValidator<Source> getValidator() {
        return validator;
    }

    @Override
    protected String getSourceType() {
        return SlackSource.TYPE;
    }

    @Test
    void isInvalidWithNoParameters() {
        Map<String, String> params = new HashMap<>();
        assertValidationIsInvalid(sourceWith(params), "$.slack_channel: is missing but it is required and $.slack_token: is missing but it is required");
    }

    @Test
    void isInvalidWithMissingChannelParameter() {
        Map<String, String> params = new HashMap<>();
        params.put(SlackSource.TOKEN_PARAM, "t");
        assertValidationIsInvalid(sourceWith(params), "$.slack_channel: is missing but it is required");
    }

    @Test
    void isInvalidWithMissingTokenParameter() {
        Map<String, String> params = new HashMap<>();
        params.put(SlackSource.CHANNEL_PARAM, "c");
        assertValidationIsInvalid(sourceWith(params), "$.slack_token: is missing but it is required");
    }

    @Test
    void isValidWithBothParameters() {
        Map<String, String> params = new HashMap<>();
        params.put(SlackSource.CHANNEL_PARAM, "channel");
        params.put(SlackSource.TOKEN_PARAM, "token");
        assertIsValid(sourceWith(params));
    }

    private void assertIsValid(Source Source) {
        ValidationResult validationResult = validator.isValid(Source);
        assertThat(validationResult.isValid()).isTrue();
    }
}
