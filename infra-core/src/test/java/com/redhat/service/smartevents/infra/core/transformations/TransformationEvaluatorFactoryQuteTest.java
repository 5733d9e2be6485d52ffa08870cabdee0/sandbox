package com.redhat.service.smartevents.infra.core.transformations;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.core.validations.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;

public class TransformationEvaluatorFactoryQuteTest {

    @Test
    public void validateTemplate() {
        TransformationEvaluatorFactoryQute factory = new TransformationEvaluatorFactoryQute();
        String template = "Hi {key} how are you?";

        ValidationResult result = factory.validate(template);
        assertThat(result).isNotNull();
        assertThat(result.isValid()).isTrue();
        assertThat(result.getViolations()).isEmpty();
    }

    @Test
    public void validateInvalidTemplate() {
        TransformationEvaluatorFactoryQute factory = new TransformationEvaluatorFactoryQute();
        String template = "Hi {key how are you?";

        ValidationResult result = factory.validate(template);
        assertThat(result).isNotNull();
        assertThat(result.isValid()).isFalse();
        assertThat(result.getViolations()).isNotEmpty();
    }
}
