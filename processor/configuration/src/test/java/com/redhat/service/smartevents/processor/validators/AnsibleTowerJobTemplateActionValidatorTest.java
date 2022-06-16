package com.redhat.service.smartevents.processor.validators;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.redhat.service.smartevents.processor.actions.ansible.AnsibleTowerJobTemplateAction;
import com.redhat.service.smartevents.processor.resolvers.AbstractGatewayValidatorTest;
import com.redhat.service.smartevents.processor.validators.custom.AnsibleTowerJobTemplateActionValidator;

import io.quarkus.test.junit.QuarkusTest;

import static com.redhat.service.smartevents.processor.actions.ansible.AnsibleTowerJobTemplateAction.BASIC_AUTH_PASSWORD_PARAM;
import static com.redhat.service.smartevents.processor.actions.ansible.AnsibleTowerJobTemplateAction.BASIC_AUTH_USERNAME_PARAM;
import static com.redhat.service.smartevents.processor.actions.ansible.AnsibleTowerJobTemplateAction.ENDPOINT_PARAM;
import static com.redhat.service.smartevents.processor.actions.ansible.AnsibleTowerJobTemplateAction.JOB_TEMPLATE_ID_PARAM;
import static com.redhat.service.smartevents.processor.actions.ansible.AnsibleTowerJobTemplateAction.SSL_VERIFICATION_DISABLED;
import static com.redhat.service.smartevents.processor.validators.custom.WebhookActionValidator.BASIC_AUTH_CONFIGURATION_MESSAGE;
import static com.redhat.service.smartevents.processor.validators.custom.WebhookActionValidator.INVALID_PROTOCOL_MESSAGE;
import static com.redhat.service.smartevents.processor.validators.custom.WebhookActionValidator.MALFORMED_ENDPOINT_PARAM_MESSAGE;

@QuarkusTest
class AnsibleTowerJobTemplateActionValidatorTest extends AbstractGatewayValidatorTest {

    static final String VALID_HTTP_ENDPOINT = "http://www.example.com/webhook";
    static final String VALID_HTTPS_ENDPOINT = "https://www.example.com/webhook";
    static final String VALID_JOB_TEMPLATE_ID = "12";
    static final String VALID_USERNAME = "test_username";
    static final String VALID_PASSWORD = "test_password";

    static final String INVALID_ENDPOINT_NOT_URL = "this-is-not-a-valid-url";
    static final String INVALID_ENDPOINT_MISSING_PROTOCOL = "www.example.com/webhook";
    static final String INVALID_ENDPOINT_UNKNOWN_PROTOCOL = "pizza://www.example.com/webhook";
    static final String INVALID_ENDPOINT_WRONG_PROTOCOL = "ftp://www.example.com/webhook";

    private static final Object[][] INVALID_PARAMS = {
            { paramMap(null, null, null, null, null), "$.endpoint: is missing but it is required" },
            { paramMap(VALID_HTTP_ENDPOINT, null, null, null, null), "$.job_template_id: is missing but it is required" },
            { paramMap(INVALID_ENDPOINT_NOT_URL, VALID_JOB_TEMPLATE_ID, null, null, null), MALFORMED_ENDPOINT_PARAM_MESSAGE },
            { paramMap(INVALID_ENDPOINT_MISSING_PROTOCOL, VALID_JOB_TEMPLATE_ID, null, null, null), MALFORMED_ENDPOINT_PARAM_MESSAGE },
            { paramMap(INVALID_ENDPOINT_UNKNOWN_PROTOCOL, VALID_JOB_TEMPLATE_ID, null, null, null), MALFORMED_ENDPOINT_PARAM_MESSAGE },
            { paramMap(INVALID_ENDPOINT_WRONG_PROTOCOL, VALID_JOB_TEMPLATE_ID, null, null, null), INVALID_PROTOCOL_MESSAGE },
            { paramMap(VALID_HTTP_ENDPOINT, VALID_JOB_TEMPLATE_ID, VALID_USERNAME, null, null), BASIC_AUTH_CONFIGURATION_MESSAGE },
            { paramMap(VALID_HTTP_ENDPOINT, VALID_JOB_TEMPLATE_ID, null, VALID_PASSWORD, null), BASIC_AUTH_CONFIGURATION_MESSAGE },
            { paramMap(VALID_HTTP_ENDPOINT, VALID_JOB_TEMPLATE_ID, VALID_USERNAME, "", null), BASIC_AUTH_CONFIGURATION_MESSAGE },
            { paramMap(VALID_HTTP_ENDPOINT, VALID_JOB_TEMPLATE_ID, "", VALID_PASSWORD, null), BASIC_AUTH_CONFIGURATION_MESSAGE },
            { paramMap(VALID_HTTP_ENDPOINT, VALID_JOB_TEMPLATE_ID, "", "", null), BASIC_AUTH_CONFIGURATION_MESSAGE },
    };

    private static final Object[] VALID_PARAMS = {
            paramMap(VALID_HTTP_ENDPOINT, VALID_JOB_TEMPLATE_ID, null, null, null),
            paramMap(VALID_HTTP_ENDPOINT, VALID_JOB_TEMPLATE_ID, VALID_USERNAME, VALID_PASSWORD, null),
            paramMap(VALID_HTTP_ENDPOINT, VALID_JOB_TEMPLATE_ID, VALID_USERNAME, VALID_PASSWORD, true),
            paramMap(VALID_HTTP_ENDPOINT, VALID_JOB_TEMPLATE_ID, VALID_USERNAME, VALID_PASSWORD, false),
            paramMap(VALID_HTTPS_ENDPOINT, VALID_JOB_TEMPLATE_ID, null, null, null),
            paramMap(VALID_HTTPS_ENDPOINT, VALID_JOB_TEMPLATE_ID, VALID_USERNAME, VALID_PASSWORD, null),
            paramMap(VALID_HTTPS_ENDPOINT, VALID_JOB_TEMPLATE_ID, VALID_USERNAME, VALID_PASSWORD, true),
            paramMap(VALID_HTTPS_ENDPOINT, VALID_JOB_TEMPLATE_ID, VALID_USERNAME, VALID_PASSWORD, false)
    };

    @Inject
    AnsibleTowerJobTemplateActionValidator validator;

    @ParameterizedTest
    @MethodSource("validParams")
    void isValid(Map<String, String> validParams) {
        assertValidationIsValid(actionWith(AnsibleTowerJobTemplateAction.TYPE, validParams));
    }

    @ParameterizedTest
    @MethodSource("invalidParams")
    void isInvalid(Map<String, String> invalidParams, String expectedErrorMessage) {
        assertValidationIsInvalid(actionWith(AnsibleTowerJobTemplateAction.TYPE, invalidParams), expectedErrorMessage);
    }

    private static Stream<Arguments> invalidParams() {
        return Arrays.stream(INVALID_PARAMS).map(Arguments::of);
    }

    private static Stream<Arguments> validParams() {
        return Arrays.stream(VALID_PARAMS).map(Arguments::of);
    }

    private static Map<String, Object> paramMap(String endpoint, String jobTemplateId, String basicUsername, String basicAuthPassword, Boolean disableSslVerification) {
        Map<String, Object> params = new HashMap<>();

        if (endpoint != null) {
            params.put(ENDPOINT_PARAM, endpoint);
        }
        if (jobTemplateId != null) {
            params.put(JOB_TEMPLATE_ID_PARAM, jobTemplateId);
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
        return params;
    }

    @Override
    protected GatewayValidator getValidator() {
        return validator;
    }
}
