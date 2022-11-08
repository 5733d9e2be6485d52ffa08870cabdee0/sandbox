package com.redhat.service.smartevents.manager.v1.api.user.validators.processors;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.hibernate.validator.constraintvalidation.HibernateConstraintViolationBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.redhat.service.smartevents.infra.v1.api.models.transformations.TransformationEvaluatorFactoryQute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransformationTemplateConstraintValidatorTest {

    TransformationTemplateConstraintValidator constraintValidator;

    @Mock
    HibernateConstraintValidatorContext validatorContextMock;
    @Mock
    HibernateConstraintViolationBuilder builderMock;

    @BeforeEach
    public void beforeEach() {
        constraintValidator = new TransformationTemplateConstraintValidator();
        constraintValidator.transformationEvaluatorFactory = new TransformationEvaluatorFactoryQute();
    }

    @Test
    void isValidNullTemplate() {
        assertThat(constraintValidator.isValid(null, validatorContextMock)).isTrue();
    }

    @Test
    void isValidEmptyTemplate() {
        assertThat(constraintValidator.isValid("", validatorContextMock)).isTrue();
    }

    @Test
    void isValidMalformedTemplate() {
        when(validatorContextMock.buildConstraintViolationWithTemplate(any(String.class))).thenReturn(builderMock);
        when(validatorContextMock.unwrap(HibernateConstraintValidatorContext.class)).thenReturn(validatorContextMock);
        when(validatorContextMock.addMessageParameter(any(), anyString())).thenReturn(validatorContextMock);
        when(validatorContextMock.withDynamicPayload(any())).thenReturn(validatorContextMock);

        assertThat(constraintValidator.isValid("Hi {key how are you?", validatorContextMock)).isFalse();

        ArgumentCaptor<String> messageCap = ArgumentCaptor.forClass(String.class);

        verify(validatorContextMock).disableDefaultConstraintViolation();
        verify(validatorContextMock).buildConstraintViolationWithTemplate(messageCap.capture());
        verify(validatorContextMock).addMessageParameter(eq(TransformationTemplateConstraintValidator.ERROR_PARAM), anyString());
        verify(builderMock).addConstraintViolation();

        assertThat(messageCap.getValue()).startsWith("Transformation template malformed:");
    }
}
