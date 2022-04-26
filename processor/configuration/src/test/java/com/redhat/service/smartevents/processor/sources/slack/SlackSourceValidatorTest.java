package com.redhat.service.smartevents.processor.sources.slack;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.infra.validations.ValidationResult;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class SlackSourceValidatorTest {

    @Inject
    SlackSourceValidator validator;

    @Test
    void isInvalidWithNoParameters() {
        Map<String, String> params = new HashMap<>();
        assertIsInvalid(SourceWith(params), SlackSourceValidator.INVALID_CHANNEL_MESSAGE);
    }

    @Test
    void isInvalidWithMissingChannelParameter() {
        Map<String, String> params = new HashMap<>();
        params.put(SlackSource.TOKEN_PARAM, "t");
        assertIsInvalid(SourceWith(params), SlackSourceValidator.INVALID_CHANNEL_MESSAGE);
    }

    @Test
    void isInvalidWithEmptyChannelParameter() {
        Map<String, String> params = new HashMap<>();
        params.put(SlackSource.CHANNEL_PARAM, "");
        params.put(SlackSource.TOKEN_PARAM, "t");
        assertIsInvalid(SourceWith(params), SlackSourceValidator.INVALID_CHANNEL_MESSAGE);
    }

    @Test
    void isInvalidWithMissingTokenParameter() {
        Map<String, String> params = new HashMap<>();
        params.put(SlackSource.CHANNEL_PARAM, "c");
        assertIsInvalid(SourceWith(params), SlackSourceValidator.INVALID_TOKEN_MESSAGE);
    }

    @Test
    void isInvalidWithEmptyTokenParameter() {
        Map<String, String> params = new HashMap<>();
        params.put(SlackSource.CHANNEL_PARAM, "c");
        params.put(SlackSource.TOKEN_PARAM, "");
        assertIsInvalid(SourceWith(params), SlackSourceValidator.INVALID_TOKEN_MESSAGE);
    }

    @Test
    void isValidWithBothParameters() {
        Map<String, String> params = new HashMap<>();
        params.put(SlackSource.CHANNEL_PARAM, "channel");
        params.put(SlackSource.TOKEN_PARAM, "token");
        assertIsValid(SourceWith(params));
    }

    private void assertIsValid(Source Source) {
        ValidationResult validationResult = validator.isValid(Source);
        assertThat(validationResult.isValid()).isTrue();
    }

    private void assertIsInvalid(Source Source, String errorMessage) {
        ValidationResult validationResult = validator.isValid(Source);
        assertThat(validationResult.isValid()).isFalse();
        if (errorMessage == null) {
            assertThat(validationResult.getMessage()).isNull();
        } else {
            assertThat(validationResult.getMessage()).startsWith(errorMessage);
        }
    }

    private Source SourceWith(Map<String, String> params) {
        Source source = new Source();
        source.setType(SlackSource.TYPE);
        source.setParameters(params);
        return source;
    }
}
