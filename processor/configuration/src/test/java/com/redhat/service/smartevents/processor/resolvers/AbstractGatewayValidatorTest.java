package com.redhat.service.smartevents.processor.resolvers;

import java.util.List;
import java.util.Map;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.validations.ValidationResult;
import com.redhat.service.smartevents.processor.validators.ActionValidator;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractGatewayValidatorTest {

    protected abstract ActionValidator getValidator();

    protected static Action actionWith(String type, Map<String, String> params) {
        Action action = new Action();
        action.setType(type);
        action.setMapParameters(params);
        return action;
    }

    protected void assertValidationIsInvalid(Action action, List<String> errorMessages) {
        ValidationResult validationResult = getValidator().isValid(action);
        assertThat(validationResult.isValid()).isFalse();
        for (String errorMessage : errorMessages) {
            assertThat(validationResult.getViolations().stream().map(v -> v.getException().getMessage()).anyMatch(m -> m.startsWith(errorMessage))).isTrue();
        }
    }

    protected void assertValidationIsValid(Action action) {
        ValidationResult validationResult = getValidator().isValid(action);
        assertThat(validationResult.isValid()).isTrue();
    }
}
