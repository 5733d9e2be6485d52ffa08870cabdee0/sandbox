package com.redhat.service.smartevents.processor.actions.sendtobridge;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.validations.ValidationResult;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class SendToBridgeActionValidatorTest {

    private final static Map<String, String> NULL_PARAMS = null;
    private final static Map<String, String> EMPTY_PARAMS = Collections.emptyMap();

    @Inject
    SendToBridgeActionValidator validator;

    @Test
    void isInvalidWithNullParams() {
        assertIsInvalid(actionWith(NULL_PARAMS), SendToBridgeActionValidator.INVALID_BRIDGE_ID_PARAM_MESSAGE);
    }

    @Test
    void isInvalidWithEmptyBridgeIdParam() {
        assertIsInvalid(actionWith(paramsWithBridgeId("")), SendToBridgeActionValidator.INVALID_BRIDGE_ID_PARAM_MESSAGE);
    }

    @Test
    void isValidWithEmptyParams() {
        assertIsInvalid(actionWith(EMPTY_PARAMS), SendToBridgeActionValidator.INVALID_BRIDGE_ID_PARAM_MESSAGE);
    }

    @Test
    void isValidWithNonEmptyBridgeIdParam() {
        assertIsValid(actionWith(paramsWithBridgeId("test-bridge-id")));
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
        Action b = new Action();
        b.setType(SendToBridgeAction.TYPE);
        b.setMapParameters(params);
        return b;
    }

    private Map<String, String> paramsWithBridgeId(String bridgeId) {
        Map<String, String> params = new HashMap<>();
        params.put(SendToBridgeAction.BRIDGE_ID_PARAM, bridgeId);
        return params;
    }
}
