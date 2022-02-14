package com.redhat.service.bridge.manager.api.user.validators.actions;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.hibernate.validator.constraintvalidation.HibernateConstraintViolationBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.service.bridge.actions.ActionParameterValidator;
import com.redhat.service.bridge.actions.ActionProvider;
import com.redhat.service.bridge.actions.ActionProviderFactory;
import com.redhat.service.bridge.actions.ValidationResult;
import com.redhat.service.bridge.infra.exceptions.definitions.user.ActionProviderException;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.manager.api.models.requests.ProcessorRequest;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
public class ActionParamValidatorContainerTest {

    public static String TEST_ACTION_TYPE = "TestAction";
    public static String TEST_ACTION_NAME = "TestActionName";
    public static String TEST_PARAM_NAME = "test-param-name";
    public static String TEST_PARAM_VALUE = "test-param-value";

    @Inject
    ActionParamValidatorContainer container;

    @InjectMock
    ActionProviderFactory actionProviderFactoryMock;

    ActionParameterValidator actionValidatorMock;

    HibernateConstraintValidatorContext validatorContext;

    HibernateConstraintViolationBuilder builderMock;

    private ProcessorRequest buildTestRequest() {
        ProcessorRequest p = new ProcessorRequest();
        BaseAction b = new BaseAction();
        b.setType(TEST_ACTION_TYPE);
        b.setName(TEST_ACTION_NAME);
        Map<String, String> params = new HashMap<>();
        params.put(TEST_PARAM_NAME, TEST_PARAM_VALUE);
        b.setParameters(params);
        p.setAction(b);
        return p;
    }

    @BeforeEach
    public void beforeEach() {
        actionValidatorMock = mock(ActionParameterValidator.class);
        when(actionValidatorMock.isValid(any())).thenReturn(ValidationResult.valid());

        ActionProvider actionProviderMock = mock(ActionProvider.class);
        when(actionProviderMock.getParameterValidator()).thenReturn(actionValidatorMock);

        reset(actionProviderFactoryMock);
        when(actionProviderFactoryMock.getActionProvider(TEST_ACTION_TYPE)).thenReturn(actionProviderMock);
        when(actionProviderFactoryMock.getActionProvider(not(eq(TEST_ACTION_TYPE)))).thenThrow(new ActionProviderException("No action provider found"));

        validatorContext = mock(HibernateConstraintValidatorContext.class);
        builderMock = mock(HibernateConstraintViolationBuilder.class);
        when(validatorContext.buildConstraintViolationWithTemplate(any(String.class))).thenReturn(builderMock);
        when(validatorContext.unwrap(HibernateConstraintValidatorContext.class)).thenReturn(validatorContext);
    }

    @Test
    public void isValid() {
        ProcessorRequest p = buildTestRequest();

        assertThat(container.isValid(p, validatorContext)).isTrue();
        verify(actionProviderFactoryMock).getActionProvider(TEST_ACTION_TYPE);
        verify(actionValidatorMock).isValid(any());
    }

    @Test
    public void isValid_nullActionIsNotValid() {
        ProcessorRequest p = buildTestRequest();
        p.setAction(null);

        assertThat(container.isValid(p, validatorContext)).isFalse();
        verify(actionProviderFactoryMock, never()).getActionProvider(any());
        verify(actionValidatorMock, never()).isValid(any());
    }

    @Test
    public void isValid_actionWithNullParametersIsNotValid() {
        ProcessorRequest p = buildTestRequest();
        p.getAction().setParameters(null);

        assertThat(container.isValid(p, validatorContext)).isFalse();
        verify(actionProviderFactoryMock, never()).getActionProvider(any());
        verify(actionValidatorMock, never()).isValid(any());
    }

    @Test
    public void isValid_actionWithEmptyParamsIsValid() {
        ProcessorRequest p = buildTestRequest();
        p.getAction().getParameters().clear();

        assertThat(container.isValid(p, validatorContext)).isTrue();
        verify(actionProviderFactoryMock).getActionProvider(TEST_ACTION_TYPE);
        verify(actionValidatorMock).isValid(any());
    }

    @Test
    public void isValid_unrecognisedActionTypeIsNotValid() {
        String doesNotExistType = "doesNotExist";

        ProcessorRequest p = buildTestRequest();
        p.getAction().setType(doesNotExistType);

        assertThat(container.isValid(p, validatorContext)).isFalse();
        verify(actionProviderFactoryMock).getActionProvider(doesNotExistType);
        verify(actionValidatorMock, never()).isValid(any());

        verify(validatorContext).disableDefaultConstraintViolation();
        verify(validatorContext).buildConstraintViolationWithTemplate(anyString());
        verify(validatorContext).addMessageParameter(ActionParamValidatorContainer.TYPE_PARAM, doesNotExistType);
        verify(builderMock).addConstraintViolation();
    }

    @Test
    public void isValid_messageFromActionValidatorAddedOnFailure() {
        String testErrorMessage = "This is a test error message returned from action validator";
        when(actionValidatorMock.isValid(any())).thenReturn(ValidationResult.invalid(testErrorMessage));

        ProcessorRequest p = buildTestRequest();

        assertThat(container.isValid(p, validatorContext)).isFalse();
        verify(actionProviderFactoryMock).getActionProvider(TEST_ACTION_TYPE);
        verify(actionValidatorMock).isValid(any());

        ArgumentCaptor<String> messageCap = ArgumentCaptor.forClass(String.class);

        verify(validatorContext).disableDefaultConstraintViolation();
        verify(validatorContext).buildConstraintViolationWithTemplate(messageCap.capture());
        verify(builderMock).addConstraintViolation();

        assertThat(messageCap.getValue()).isEqualTo(testErrorMessage);
    }

    @Test
    public void isValid_noMessageFromValidatorGenericMessageAdded() {

        when(actionValidatorMock.isValid(any())).thenReturn(ValidationResult.invalid(null));

        ProcessorRequest p = buildTestRequest();

        assertThat(container.isValid(p, validatorContext)).isFalse();
        verify(actionProviderFactoryMock).getActionProvider(TEST_ACTION_TYPE);
        verify(actionValidatorMock).isValid(any());

        ArgumentCaptor<String> messageCap = ArgumentCaptor.forClass(String.class);

        verify(validatorContext).disableDefaultConstraintViolation();
        verify(validatorContext).buildConstraintViolationWithTemplate(messageCap.capture());
        verify(validatorContext).addMessageParameter(ActionParamValidatorContainer.TYPE_PARAM, TEST_ACTION_TYPE);
        verify(validatorContext).addMessageParameter(ActionParamValidatorContainer.NAME_PARAM, TEST_ACTION_NAME);
        verify(builderMock).addConstraintViolation();

        assertThat(messageCap.getValue()).isEqualTo(ActionParamValidatorContainer.ACTION_PARAMETERS_NOT_VALID_ERROR);
    }
}
