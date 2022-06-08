package com.redhat.service.smartevents.processor.validators;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.validations.ValidationResult;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;
import com.redhat.service.smartevents.processor.validators.custom.WebhookActionValidator;

import io.quarkus.test.junit.QuarkusTest;

import static com.redhat.service.smartevents.processor.actions.webhook.WebhookAction.BASIC_AUTH_PASSWORD_PARAM;
import static com.redhat.service.smartevents.processor.actions.webhook.WebhookAction.BASIC_AUTH_USERNAME_PARAM;
import static com.redhat.service.smartevents.processor.actions.webhook.WebhookAction.ENDPOINT_PARAM;
import static com.redhat.service.smartevents.processor.actions.webhook.WebhookAction.SSL_VERIFICATION_DISABLED;
import static com.redhat.service.smartevents.processor.actions.webhook.WebhookAction.USE_TECHNICAL_BEARER_TOKEN_PARAM;
import static com.redhat.service.smartevents.processor.validators.custom.WebhookActionValidator.BASIC_AUTH_CONFIGURATION_MESSAGE;
import static com.redhat.service.smartevents.processor.validators.custom.WebhookActionValidator.INVALID_PROTOCOL_MESSAGE;
import static com.redhat.service.smartevents.processor.validators.custom.WebhookActionValidator.MALFORMED_ENDPOINT_PARAM_MESSAGE;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class WebhookActionValidatorTest {

    static final String VALID_HTTP_ENDPOINT = "http://www.example.com/webhook";
    static final String VALID_HTTPS_ENDPOINT = "https://www.example.com/webhook";
    static final String VALID_USERNAME = "test_username";
    static final String VALID_PASSWORD = "test_password";

    static final String INVALID_ENDPOINT_NOT_URL = "this-is-not-a-valid-url";
    static final String INVALID_ENDPOINT_MISSING_PROTOCOL = "www.example.com/webhook";
    static final String INVALID_ENDPOINT_UNKNOWN_PROTOCOL = "pizza://www.example.com/webhook";
    static final String INVALID_ENDPOINT_WRONG_PROTOCOL = "ftp://www.example.com/webhook";

    private static final Object[][] INVALID_PARAMS = {
            { paramMap(null, null, null, null, null), "$.endpoint: is missing but it is required" },
            { paramMap(INVALID_ENDPOINT_NOT_URL, null, null, null, null), MALFORMED_ENDPOINT_PARAM_MESSAGE },
            { paramMap(INVALID_ENDPOINT_MISSING_PROTOCOL, null, null, null, null), MALFORMED_ENDPOINT_PARAM_MESSAGE },
            { paramMap(INVALID_ENDPOINT_UNKNOWN_PROTOCOL, null, null, null, null), MALFORMED_ENDPOINT_PARAM_MESSAGE },
            { paramMap(INVALID_ENDPOINT_WRONG_PROTOCOL, null, null, null, null), INVALID_PROTOCOL_MESSAGE },
            { paramMap(VALID_HTTP_ENDPOINT, VALID_USERNAME, null, null, null), BASIC_AUTH_CONFIGURATION_MESSAGE },
            { paramMap(VALID_HTTP_ENDPOINT, null, VALID_PASSWORD, null, null), BASIC_AUTH_CONFIGURATION_MESSAGE },
            { paramMap(VALID_HTTP_ENDPOINT, VALID_USERNAME, "", null, null), BASIC_AUTH_CONFIGURATION_MESSAGE },
            { paramMap(VALID_HTTP_ENDPOINT, "", VALID_PASSWORD, null, null), BASIC_AUTH_CONFIGURATION_MESSAGE },
            { paramMap(VALID_HTTP_ENDPOINT, "", "", null, null), BASIC_AUTH_CONFIGURATION_MESSAGE },
            { paramMap(VALID_HTTP_ENDPOINT, VALID_USERNAME, VALID_PASSWORD, null, "value"),
                    "$.useTechnicalBearerToken: is not defined in the schema and the schema does not allow additional properties" }
    };

    private static final Object[] VALID_PARAMS = {
            paramMap(VALID_HTTP_ENDPOINT, null, null, null, null),
            paramMap(VALID_HTTP_ENDPOINT, VALID_USERNAME, VALID_PASSWORD, null, null),
            paramMap(VALID_HTTP_ENDPOINT, VALID_USERNAME, VALID_PASSWORD, true, null),
            paramMap(VALID_HTTP_ENDPOINT, VALID_USERNAME, VALID_PASSWORD, false, null),
            paramMap(VALID_HTTPS_ENDPOINT, null, null, null, null),
            paramMap(VALID_HTTPS_ENDPOINT, VALID_USERNAME, VALID_PASSWORD, null, null),
            paramMap(VALID_HTTPS_ENDPOINT, VALID_USERNAME, VALID_PASSWORD, true, null),
            paramMap(VALID_HTTPS_ENDPOINT, VALID_USERNAME, VALID_PASSWORD, false, null)
    };

    @Inject
    WebhookActionValidator validator;

    @ParameterizedTest
    @MethodSource("validParams")
    void isValid(Map<String, String> params) {
        ValidationResult validationResult = validator.isValid(actionWith(params));
        assertThat(validationResult.isValid()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("invalidParams")
    void isInvalid(Map<String, String> params, String expectedErrorMessage) {
        ValidationResult validationResult = validator.isValid(actionWith(params));
        assertThat(validationResult.isValid()).isFalse();
        assertThat(validationResult.getMessage()).startsWith(expectedErrorMessage);
    }

    @Test
    void isInvalidWithNullParametersMap() {
        Action action = new Action();
        action.setType(WebhookAction.TYPE);
        action.setMapParameters(new HashMap<>());
        ValidationResult validationResult = validator.isValid(action);
        assertThat(validationResult.isValid()).isFalse();
    }

    private static Stream<Arguments> invalidParams() {
        return Arrays.stream(INVALID_PARAMS).map(Arguments::of);
    }

    private static Stream<Arguments> validParams() {
        return Arrays.stream(VALID_PARAMS).map(Arguments::of);
    }

    private static Map<String, Object> paramMap(String endpoint, String basicUsername, String basicAuthPassword, Boolean disableSslVerification, String useBearerToken) {
        Map<String, Object> params = new HashMap<>();

        if (endpoint != null) {
            params.put(ENDPOINT_PARAM, endpoint);
        }
        if (basicUsername != null) {
            params.put(BASIC_AUTH_USERNAME_PARAM, basicUsername);
        }
        if (basicAuthPassword != null) {
            params.put(BASIC_AUTH_PASSWORD_PARAM, basicAuthPassword);
        }
        if (disableSslVerification != null) {
            params.put(SSL_VERIFICATION_DISABLED, disableSslVerification);
        }
        if (useBearerToken != null) {
            params.put(USE_TECHNICAL_BEARER_TOKEN_PARAM, useBearerToken);
        }
        return params;
    }

    private static Action actionWith(Map<String, String> params) {
        Action action = new Action();
        action.setType(WebhookAction.TYPE);
        action.setMapParameters(params);
        return action;
    }
}
