package com.redhat.service.smartevents.processor.resolvers;

import java.util.Map;

import com.redhat.service.smartevents.infra.models.gateways.Gateway;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.infra.validations.ValidationResult;
import com.redhat.service.smartevents.processor.validators.GatewayValidator;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractSourceTest<T extends Gateway> {

    protected abstract GatewayValidator getValidator();

    protected abstract String getSourceType();

    protected Source sourceWith(Map<String, String> params) {
        Source source = new Source();
        source.setType(getSourceType());
        source.setMapParameters(params);
        return source;
    }

    protected void assertValidationIsInvalid(T source, String errorMessage) {
        ValidationResult validationResult = getValidator().isValid(source);
        assertThat(validationResult.isValid()).isFalse();
        assertThat(validationResult.getMessage()).startsWith(errorMessage);
    }
}
