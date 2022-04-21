package com.redhat.service.smartevents.manager.api.user.validators.processors;

import javax.inject.Inject;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.hibernate.validator.constraintvalidation.HibernateConstraintViolationBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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

    @Inject
    TransformationTemplateConstraintValidator constraintValidator;

    HibernateConstraintValidatorContext validatorContextMock;
    HibernateConstraintViolationBuilder builderMock;

    @BeforeEach
    public void beforeEach() {
        validatorContextMock = mock(HibernateConstraintValidatorContext.class);
        builderMock = mock(HibernateConstraintViolationBuilder.class);
        when(validatorContextMock.buildConstraintViolationWithTemplate(any(String.class))).thenReturn(builderMock);
        when(validatorContextMock.unwrap(HibernateConstraintValidatorContext.class)).thenReturn(validatorContextMock);
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
        assertThat(constraintValidator.isValid("Hi {key how are you?", validatorContextMock)).isFalse();

        ArgumentCaptor<String> messageCap = ArgumentCaptor.forClass(String.class);

        verify(validatorContextMock).disableDefaultConstraintViolation();
        verify(validatorContextMock).buildConstraintViolationWithTemplate(messageCap.capture());
        verify(validatorContextMock).addMessageParameter(eq(TransformationTemplateConstraintValidator.ERROR_PARAM), anyString());
        verify(builderMock).addConstraintViolation();

        assertThat(messageCap.getValue()).startsWith("Transformation template malformed:");
    }
}
