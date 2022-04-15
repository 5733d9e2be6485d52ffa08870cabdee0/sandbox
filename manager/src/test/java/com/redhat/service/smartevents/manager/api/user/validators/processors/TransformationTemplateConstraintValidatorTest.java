package com.redhat.service.smartevents.manager.api.user.validators.processors;

import javax.inject.Inject;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.hibernate.validator.constraintvalidation.HibernateConstraintViolationBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.manager.api.models.requests.ProcessorRequest;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class TransformationTemplateConstraintValidatorTest {

    public static final String TEST_ACTION_TYPE = "TestAction";
    public static final String TEST_PROCESSOR_NAME = "processor-name";

    @Inject
    TransformationTemplateConstraintValidator container;

    HibernateConstraintValidatorContext validatorContext;

    HibernateConstraintViolationBuilder builderMock;

    private ProcessorRequest buildTestRequest(String transformationTemplate) {
        Action b = new Action();
        b.setType(TEST_ACTION_TYPE);
        return new ProcessorRequest(TEST_PROCESSOR_NAME, null, transformationTemplate, b);
    }

    @BeforeEach
    public void beforeEach() {
        validatorContext = mock(HibernateConstraintValidatorContext.class);
        builderMock = mock(HibernateConstraintViolationBuilder.class);
        when(validatorContext.buildConstraintViolationWithTemplate(any(String.class))).thenReturn(builderMock);
        when(validatorContext.unwrap(HibernateConstraintValidatorContext.class)).thenReturn(validatorContext);
    }

    @Test
    void isValidNullTemplate() {
        assertThat(container.isValid(null, validatorContext)).isTrue();
    }

    @Test
    void isValidEmptyTemplate() {
        assertThat(container.isValid("", validatorContext)).isTrue();
    }

    @Test
    void isValidMalformedTemplate() {
        assertThat(container.isValid("Hi {key how are you?", validatorContext)).isFalse();

        ArgumentCaptor<String> messageCap = ArgumentCaptor.forClass(String.class);

        verify(validatorContext).disableDefaultConstraintViolation();
        verify(validatorContext).buildConstraintViolationWithTemplate(messageCap.capture());
        verify(validatorContext).addMessageParameter(eq(TransformationTemplateConstraintValidator.ERROR_PARAM), anyString());
        verify(builderMock).addConstraintViolation();

        assertThat(messageCap.getValue()).startsWith("Transformation template malformed:");
    }
}
