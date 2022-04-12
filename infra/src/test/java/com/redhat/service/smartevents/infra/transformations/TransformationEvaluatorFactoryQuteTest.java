package com.redhat.service.smartevents.infra.transformations;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.validations.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;

public class TransformationEvaluatorFactoryQuteTest {

    @Test
    public void validateTemplate() {
        TransformationEvaluatorFactoryQute factory = new TransformationEvaluatorFactoryQute();
        String template = "Hi {key} how are you?";

        ValidationResult result = factory.validate(template);
        assertThat(result).isNotNull();
        assertThat(result.isValid()).isTrue();
        assertThat(result.getMessage()).isNull();
    }

    @Test
    public void validateInvalidTemplate() {
        TransformationEvaluatorFactoryQute factory = new TransformationEvaluatorFactoryQute();
        String template = "Hi {key how are you?";

        ValidationResult result = factory.validate(template);
        assertThat(result).isNotNull();
        assertThat(result.isValid()).isFalse();
        assertThat(result.getMessage()).isNotBlank();
    }
}
