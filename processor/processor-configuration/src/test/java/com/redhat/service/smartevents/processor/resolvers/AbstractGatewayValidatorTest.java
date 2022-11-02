package com.redhat.service.smartevents.processor.resolvers;

import java.util.List;
import java.util.Map;

import com.redhat.service.smartevents.infra.core.models.gateways.Action;
import com.redhat.service.smartevents.infra.core.models.gateways.Gateway;
import com.redhat.service.smartevents.infra.core.models.gateways.Source;
import com.redhat.service.smartevents.infra.core.validations.ValidationResult;
import com.redhat.service.smartevents.processor.validators.GatewayValidator;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractGatewayValidatorTest {

    protected abstract GatewayValidator getValidator();

    protected Source sourceWith(String type, Map<String, String> params) {
        Source source = new Source();
        source.setType(type);
        source.setMapParameters(params);
        return source;
    }

    protected static Action actionWith(String type, Map<String, String> params) {
        Action action = new Action();
        action.setType(type);
        action.setMapParameters(params);
        return action;
    }

    protected void assertValidationIsInvalid(Gateway gateway, List<String> errorMessages) {
        ValidationResult validationResult = getValidator().isValid(gateway);
        assertThat(validationResult.isValid()).isFalse();
        for (String errorMessage : errorMessages) {
            assertThat(validationResult.getViolations().stream().map(v -> v.getException().getMessage()).anyMatch(m -> m.startsWith(errorMessage))).isTrue();
        }
    }

    protected void assertValidationIsValid(Gateway gateway) {
        ValidationResult validationResult = getValidator().isValid(gateway);
        assertThat(validationResult.isValid()).isTrue();
    }
}
