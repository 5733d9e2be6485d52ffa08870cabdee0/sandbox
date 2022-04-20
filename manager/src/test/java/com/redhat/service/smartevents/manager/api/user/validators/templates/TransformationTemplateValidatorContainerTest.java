package com.redhat.service.smartevents.manager.api.user.validators.templates;

import javax.inject.Inject;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.hibernate.validator.constraintvalidation.HibernateConstraintViolationBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.service.smartevents.infra.models.actions.Action;
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
public class TransformationTemplateValidatorContainerTest {

    public static final String TEST_ACTION_TYPE = "TestAction";
    public static final String TEST_PROCESSOR_NAME = "processor-name";

    @Inject
    TransformationTemplateValidatorContainer container;

    HibernateConstraintValidatorContext validatorContext;

    HibernateConstraintViolationBuilder builderMock;

    private ProcessorRequest buildTestRequest(String transformationTemplate) {
        Action action = new Action();
        action.setType(TEST_ACTION_TYPE);
        return new ProcessorRequest(TEST_PROCESSOR_NAME, null, transformationTemplate, action);
    }

    @BeforeEach
    public void beforeEach() {
        validatorContext = mock(HibernateConstraintValidatorContext.class);
        builderMock = mock(HibernateConstraintViolationBuilder.class);
        when(validatorContext.buildConstraintViolationWithTemplate(any(String.class))).thenReturn(builderMock);
        when(validatorContext.unwrap(HibernateConstraintValidatorContext.class)).thenReturn(validatorContext);
    }

    @Test
    public void isValidNullTemplate() {
        ProcessorRequest p = buildTestRequest(null);

        assertThat(container.isValid(p, validatorContext)).isTrue();
    }

    @Test
    public void isValidEmptyTemplate() {
        ProcessorRequest p = buildTestRequest("");

        assertThat(container.isValid(p, validatorContext)).isTrue();
    }

    @Test
    public void isValidMalformedTemplate() {
        ProcessorRequest p = buildTestRequest("Hi {key how are you?");

        assertThat(container.isValid(p, validatorContext)).isFalse();

        ArgumentCaptor<String> messageCap = ArgumentCaptor.forClass(String.class);

        verify(validatorContext).disableDefaultConstraintViolation();
        verify(validatorContext).buildConstraintViolationWithTemplate(messageCap.capture());
        verify(validatorContext).addMessageParameter(eq(TransformationTemplateValidatorContainer.ERROR_PARAM), anyString());
        verify(builderMock).addConstraintViolation();

        assertThat(messageCap.getValue()).startsWith("Transformation template malformed:");
    }
}
