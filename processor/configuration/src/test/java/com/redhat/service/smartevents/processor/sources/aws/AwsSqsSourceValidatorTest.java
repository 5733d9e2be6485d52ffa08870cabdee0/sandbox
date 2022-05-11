package com.redhat.service.smartevents.processor.sources.aws;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.infra.validations.ValidationResult;

import io.quarkus.test.junit.QuarkusTest;

import static com.redhat.service.smartevents.processor.sources.aws.AwsSqsSource.AWS_ACCESS_KEY_ID_PARAM;
import static com.redhat.service.smartevents.processor.sources.aws.AwsSqsSource.AWS_QUEUE_URL_PARAM;
import static com.redhat.service.smartevents.processor.sources.aws.AwsSqsSource.AWS_REGION_PARAM;
import static com.redhat.service.smartevents.processor.sources.aws.AwsSqsSource.AWS_SECRET_ACCESS_KEY_PARAM;
import static com.redhat.service.smartevents.processor.sources.aws.AwsSqsSourceValidator.INVALID_AWS_REGION_PARAM_MESSAGE;
import static com.redhat.service.smartevents.processor.sources.aws.AwsSqsSourceValidator.malformedUrlMessage;
import static com.redhat.service.smartevents.processor.sources.aws.AwsSqsSourceValidator.missingParameterMessage;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class AwsSqsSourceValidatorTest {

    static final String INVALID_QUEUE_URL = "invalid";

    static final String VALID_QUEUE_NAME = "test_queue";
    static final String VALID_AWS_QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/123456789012/" + VALID_QUEUE_NAME;
    static final String VALID_GENERIC_ENDPOINT = "http://localstack.local:4566";
    static final String VALID_GENERIC_QUEUE_URL = VALID_GENERIC_ENDPOINT + "/000000000000/" + VALID_QUEUE_NAME;
    static final String INVALID_AWS_REGION = "123";
    static final String VALID_AWS_REGION = "us-east-1";
    static final String VALID_AWS_ACCESS_KEY_ID = "key";
    static final String VALID_AWS_SECRET_ACCESS_KEY = "secret";

    private static final Object[][] INVALID_PARAMS = {
            { paramMap(null, null, null, null), missingParameterMessage(AWS_QUEUE_URL_PARAM) },
            { paramMap("", null, null, null), missingParameterMessage(AWS_QUEUE_URL_PARAM) },
            { paramMap(VALID_AWS_QUEUE_URL, null, null, null), missingParameterMessage(AWS_ACCESS_KEY_ID_PARAM) },
            { paramMap(VALID_AWS_QUEUE_URL, "", null, null), missingParameterMessage(AWS_ACCESS_KEY_ID_PARAM) },
            { paramMap(VALID_AWS_QUEUE_URL, VALID_AWS_REGION, null, null), missingParameterMessage(AWS_ACCESS_KEY_ID_PARAM) },
            { paramMap(VALID_AWS_QUEUE_URL, VALID_AWS_REGION, "", null), missingParameterMessage(AWS_ACCESS_KEY_ID_PARAM) },
            { paramMap(VALID_AWS_QUEUE_URL, VALID_AWS_REGION, VALID_AWS_ACCESS_KEY_ID, null), missingParameterMessage(AWS_SECRET_ACCESS_KEY_PARAM) },
            { paramMap(VALID_AWS_QUEUE_URL, VALID_AWS_REGION, VALID_AWS_ACCESS_KEY_ID, ""), missingParameterMessage(AWS_SECRET_ACCESS_KEY_PARAM) },
            { paramMap(INVALID_QUEUE_URL, VALID_AWS_REGION, VALID_AWS_ACCESS_KEY_ID, VALID_AWS_SECRET_ACCESS_KEY), malformedUrlMessage(INVALID_QUEUE_URL) },
            { paramMap(VALID_AWS_QUEUE_URL, "", VALID_AWS_ACCESS_KEY_ID, VALID_AWS_SECRET_ACCESS_KEY), INVALID_AWS_REGION_PARAM_MESSAGE },
            { paramMap(VALID_AWS_QUEUE_URL, INVALID_AWS_REGION, VALID_AWS_ACCESS_KEY_ID, VALID_AWS_SECRET_ACCESS_KEY), INVALID_AWS_REGION_PARAM_MESSAGE }
    };

    private static final Object[] VALID_PARAMS = {
            paramMap(VALID_AWS_QUEUE_URL, null, VALID_AWS_ACCESS_KEY_ID, VALID_AWS_SECRET_ACCESS_KEY),
            paramMap(VALID_AWS_QUEUE_URL, VALID_AWS_REGION, VALID_AWS_ACCESS_KEY_ID, VALID_AWS_SECRET_ACCESS_KEY),
            paramMap(VALID_GENERIC_QUEUE_URL, null, VALID_AWS_ACCESS_KEY_ID, VALID_AWS_SECRET_ACCESS_KEY),
            paramMap(VALID_GENERIC_QUEUE_URL, VALID_AWS_REGION, VALID_AWS_ACCESS_KEY_ID, VALID_AWS_SECRET_ACCESS_KEY)
    };

    @Inject
    AwsSqsSourceValidator validator;

    @ParameterizedTest
    @MethodSource("validParams")
    void isValid(Map<String, String> params) {
        ValidationResult validationResult = validator.isValid(sourceWith(params));
        assertThat(validationResult.isValid()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("invalidParams")
    void isInvalid(Map<String, String> params, String expectedErrorMessage) {
        ValidationResult validationResult = validator.isValid(sourceWith(params));
        assertThat(validationResult.isValid()).isFalse();
        assertThat(validationResult.getMessage()).isEqualTo(expectedErrorMessage);
    }

    private static Stream<Arguments> invalidParams() {
        return Arrays.stream(INVALID_PARAMS).map(Arguments::of);
    }

    private static Stream<Arguments> validParams() {
        return Arrays.stream(VALID_PARAMS).map(Arguments::of);
    }

    static Map<String, String> paramMap(String queueUrl, String awsRegion, String awsAccessKeyId, String awsSecretAccessKey) {
        Map<String, String> params = new HashMap<>();
        if (queueUrl != null) {
            params.put(AWS_QUEUE_URL_PARAM, queueUrl);
        }
        if (awsRegion != null) {
            params.put(AWS_REGION_PARAM, awsRegion);
        }
        if (awsAccessKeyId != null) {
            params.put(AWS_ACCESS_KEY_ID_PARAM, awsAccessKeyId);
        }
        if (awsSecretAccessKey != null) {
            params.put(AWS_SECRET_ACCESS_KEY_PARAM, awsSecretAccessKey);
        }
        return params;
    }

    static Source sourceWith(Map<String, String> params) {
        Source source = new Source();
        source.setType(AwsSqsSource.TYPE);
        source.setParameters(params);
        return source;
    }
}
