package com.redhat.service.smartevents.manager.api.user.validators.processors;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.hibernate.validator.constraintvalidation.HibernateConstraintViolationBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.GatewayProviderException;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.validations.ValidationResult;
import com.redhat.service.smartevents.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.processor.GatewayConfigurator;
import com.redhat.service.smartevents.processor.GatewayValidator;

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
public class GatewayConstraintValidatorTest {

    public static String TEST_ACTION_TYPE = "TestAction";
    public static String TEST_PARAM_NAME = "test-param-name";
    public static String TEST_PARAM_VALUE = "test-param-value";

    @Inject
    GatewayConstraintValidator container;

    @InjectMock
    GatewayConfigurator gatewayConfiguratorMock;

    GatewayValidator actionValidatorMock;

    HibernateConstraintValidatorContext validatorContext;

    HibernateConstraintViolationBuilder builderMock;

    private ProcessorRequest buildTestRequest() {
        ProcessorRequest p = new ProcessorRequest();
        Action action = new Action();
        action.setType(TEST_ACTION_TYPE);
        Map<String, String> params = new HashMap<>();
        params.put(TEST_PARAM_NAME, TEST_PARAM_VALUE);
        action.setParameters(params);
        p.setAction(action);
        return p;
    }

    @BeforeEach
    public void beforeEach() {
        actionValidatorMock = mock(GatewayValidator.class);
        when(actionValidatorMock.isValid(any())).thenReturn(ValidationResult.valid());

        reset(gatewayConfiguratorMock);
        when(gatewayConfiguratorMock.getActionValidator(TEST_ACTION_TYPE)).thenReturn(actionValidatorMock);
        when(gatewayConfiguratorMock.getActionValidator(not(eq(TEST_ACTION_TYPE)))).thenThrow(new GatewayProviderException("No action provider found"));

        validatorContext = mock(HibernateConstraintValidatorContext.class);
        builderMock = mock(HibernateConstraintViolationBuilder.class);
        when(validatorContext.buildConstraintViolationWithTemplate(any(String.class))).thenReturn(builderMock);
        when(validatorContext.unwrap(HibernateConstraintValidatorContext.class)).thenReturn(validatorContext);
    }

    @Test
    public void isValid() {
        ProcessorRequest p = buildTestRequest();

        assertThat(container.isValid(p, validatorContext)).isTrue();
        verify(gatewayConfiguratorMock).getActionValidator(TEST_ACTION_TYPE);
        verify(actionValidatorMock).isValid(any());
    }

    @Test
    public void isValid_nullActionIsNotValid() {
        ProcessorRequest p = buildTestRequest();
        p.setAction(null);

        assertThat(container.isValid(p, validatorContext)).isFalse();
        verify(gatewayConfiguratorMock, never()).getActionValidator(any());
        verify(actionValidatorMock, never()).isValid(any());
    }

    @Test
    public void isValid_actionWithNullParametersIsNotValid() {
        ProcessorRequest p = buildTestRequest();
        p.getAction().setParameters(null);

        assertThat(container.isValid(p, validatorContext)).isFalse();
        verify(gatewayConfiguratorMock, never()).getActionValidator(any());
        verify(actionValidatorMock, never()).isValid(any());
    }

    @Test
    public void isValid_actionWithEmptyParamsIsValid() {
        ProcessorRequest p = buildTestRequest();
        p.getAction().getParameters().clear();

        assertThat(container.isValid(p, validatorContext)).isTrue();
        verify(gatewayConfiguratorMock).getActionValidator(TEST_ACTION_TYPE);
        verify(actionValidatorMock).isValid(any());
    }

    @Test
    public void isValid_unrecognisedActionTypeIsNotValid() {
        String doesNotExistType = "doesNotExist";

        ProcessorRequest p = buildTestRequest();
        p.getAction().setType(doesNotExistType);

        assertThat(container.isValid(p, validatorContext)).isFalse();
        verify(gatewayConfiguratorMock).getActionValidator(doesNotExistType);
        verify(actionValidatorMock, never()).isValid(any());

        verify(validatorContext).disableDefaultConstraintViolation();
        verify(validatorContext).buildConstraintViolationWithTemplate(anyString());
        verify(validatorContext).addMessageParameter(GatewayConstraintValidator.TYPE_PARAM, doesNotExistType);
        verify(builderMock).addConstraintViolation();
    }

    @Test
    public void isValid_messageFromActionValidatorAddedOnFailure() {
        String testErrorMessage = "This is a test error message returned from action validator";
        when(actionValidatorMock.isValid(any())).thenReturn(ValidationResult.invalid(testErrorMessage));

        ProcessorRequest p = buildTestRequest();

        assertThat(container.isValid(p, validatorContext)).isFalse();
        verify(gatewayConfiguratorMock).getActionValidator(TEST_ACTION_TYPE);
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
        verify(gatewayConfiguratorMock).getActionValidator(TEST_ACTION_TYPE);
        verify(actionValidatorMock).isValid(any());

        ArgumentCaptor<String> messageCap = ArgumentCaptor.forClass(String.class);

        verify(validatorContext).disableDefaultConstraintViolation();
        verify(validatorContext).buildConstraintViolationWithTemplate(messageCap.capture());
        verify(validatorContext).addMessageParameter(GatewayConstraintValidator.TYPE_PARAM, TEST_ACTION_TYPE);
        verify(builderMock).addConstraintViolation();

        assertThat(messageCap.getValue()).isEqualTo(GatewayConstraintValidator.ACTION_PARAMETERS_NOT_VALID_ERROR);
    }
}
