package com.redhat.service.smartevents.manager.api.user.validators.processors;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.hibernate.validator.constraintvalidation.HibernateConstraintViolationBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.verification.VerificationMode;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.ProcessorGatewayParametersNotValidException;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.validations.ValidationResult;
import com.redhat.service.smartevents.manager.ProcessorRequestForTests;
import com.redhat.service.smartevents.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.processor.ActionConfigurator;
import com.redhat.service.smartevents.processor.validators.ActionValidator;

import static com.redhat.service.smartevents.manager.api.user.validators.processors.ProcessorGatewayConstraintValidator.GATEWAY_CLASS_PARAM;
import static com.redhat.service.smartevents.manager.api.user.validators.processors.ProcessorGatewayConstraintValidator.GATEWAY_PARAMETERS_MISSING_ERROR;
import static com.redhat.service.smartevents.manager.api.user.validators.processors.ProcessorGatewayConstraintValidator.GATEWAY_TYPE_MISSING_ERROR;
import static com.redhat.service.smartevents.manager.api.user.validators.processors.ProcessorGatewayConstraintValidator.MISSING_ACTION_ERROR;
import static com.redhat.service.smartevents.manager.api.user.validators.processors.ProcessorGatewayConstraintValidator.TYPE_PARAM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProcessorGatewayConstraintValidatorTest {

    public static String TEST_ACTION_TYPE = "TestAction";
    public static String TEST_SOURCE_TYPE = "TestSource";
    public static String TEST_PARAM_NAME = "test-param-name";
    public static String TEST_PARAM_VALUE = "test-param-value";

    ProcessorGatewayConstraintValidator constraintValidator;

    @Mock
    ActionConfigurator actionConfiguratorMock;
    @Mock
    ActionValidator validatorMock;
    @Mock
    HibernateConstraintValidatorContext validatorContextMock;
    @Mock
    HibernateConstraintViolationBuilder builderMock;

    @BeforeEach
    public void beforeEach() {
        lenient().when(validatorMock.isValid(any(Action.class))).thenReturn(ValidationResult.valid());

        lenient().when(actionConfiguratorMock.getValidator(any(String.class))).thenReturn(validatorMock);

        lenient().when(validatorContextMock.buildConstraintViolationWithTemplate(any(String.class))).thenReturn(builderMock);
        lenient().when(validatorContextMock.unwrap(HibernateConstraintValidatorContext.class)).thenReturn(validatorContextMock);

        constraintValidator = new ProcessorGatewayConstraintValidator(actionConfiguratorMock);
    }

    @Test
    void isValid_noGatewaysIsNotValid() {
        ProcessorRequest p = new ProcessorRequest();

        assertThat(constraintValidator.isValid(p, validatorContextMock)).isFalse();
        verify(actionConfiguratorMock, never()).getValidator(any());
        verify(validatorMock, never()).isValid(any());
        verifyErrorMessage(MISSING_ACTION_ERROR);
    }

    @ParameterizedTest
    @MethodSource("gateways")
    void isValid(Action action) {
        ProcessorRequest p = buildTestRequest(action);

        assertThat(constraintValidator.isValid(p, validatorContextMock)).isTrue();
        verifyGetValidatorCall(times(1), action.getType());
        verifyIsValidCall(times(1));
    }

    @ParameterizedTest
    @MethodSource("gateways")
    void isValid_gatewayWithNullParametersIsNotValid(Action action) {
        action.setParameters(null);
        ProcessorRequest p = buildTestRequest(action);

        assertThat(constraintValidator.isValid(p, validatorContextMock)).isFalse();
        verifyGetValidatorCall(never(), ArgumentMatchers::anyString);
        verifyIsValidCall(never());
        verifyErrorMessage(GATEWAY_PARAMETERS_MISSING_ERROR, action, false);
    }

    @ParameterizedTest
    @MethodSource("gateways")
    void isValid_gatewayWithEmptyParamsIsValid(Action action) {
        action.setMapParameters(new HashMap<>());
        ProcessorRequest p = buildTestRequest(action);

        assertThat(constraintValidator.isValid(p, validatorContextMock)).isTrue();
        verifyGetValidatorCall(times(1), action.getType());
        verifyIsValidCall(times(1));
    }

    @ParameterizedTest
    @MethodSource("gateways")
    void isValid_nullGatewayTypeIsNotValid(Action action) {
        action.setType(null);
        ProcessorRequest p = buildTestRequest(action);

        assertThat(constraintValidator.isValid(p, validatorContextMock)).isFalse();
        verifyGetValidatorCall(never(), ArgumentMatchers::anyString);
        verifyIsValidCall(never());
        verifyErrorMessage(GATEWAY_TYPE_MISSING_ERROR, action, false);
    }

    @ParameterizedTest
    @MethodSource("gateways")
    void isValid_messageFromGatewayValidatorAddedOnFailure(Action action) {
        ProcessorGatewayParametersNotValidException exception = new ProcessorGatewayParametersNotValidException("This is a test error message returned from validator");
        lenient().when(validatorMock.isValid(any())).thenReturn(ValidationResult.invalid(exception));

        ProcessorRequest p = buildTestRequest(action);

        assertThat(constraintValidator.isValid(p, validatorContextMock)).isFalse();
        verifyGetValidatorCall(times(1), action.getType());
        verifyIsValidCall(times(1));
        verifyErrorMessage(exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("gateways")
    void isValid_sourceWithTransformationIsNotValid(Action action) {
        ProcessorRequestForTests p = buildTestRequest(action);
        p.setTransformationTemplate("template");

        assertThat(constraintValidator.isValid(p, validatorContextMock)).isTrue();
        verifyGetValidatorCall(times(1), action.getType());
        verifyIsValidCall(times(1));
    }

    private void verifyGetValidatorCall(VerificationMode mode, String argument) {
        verifyGetValidatorCall(mode, () -> argument);
    }

    private void verifyGetValidatorCall(VerificationMode mode, Supplier<String> argumentSupplier) {
        verify(actionConfiguratorMock, mode).getValidator(argumentSupplier.get());
    }

    private void verifyIsValidCall(VerificationMode mode) {
        verify(validatorMock, mode).isValid(any());
    }

    private void verifyErrorMessage(String expectedMessage) {
        ArgumentCaptor<String> messageCap = ArgumentCaptor.forClass(String.class);

        verify(validatorContextMock).disableDefaultConstraintViolation();
        verify(validatorContextMock).buildConstraintViolationWithTemplate(messageCap.capture());
        verify(builderMock).addConstraintViolation();

        assertThat(messageCap.getValue()).isEqualTo(expectedMessage);
    }

    private void verifyErrorMessage(String expectedMessage, Action action, boolean verifyTypeParam) {
        verifyErrorMessage(expectedMessage);
        verify(validatorContextMock).addMessageParameter(GATEWAY_CLASS_PARAM, action.getClass().getSimpleName());
        if (verifyTypeParam) {
            verify(validatorContextMock).addMessageParameter(TYPE_PARAM, action.getType());
        }
    }

    private static Action buildTestAction() {
        Action action = new Action();
        action.setType(TEST_ACTION_TYPE);
        Map<String, String> params = new HashMap<>();
        params.put(TEST_PARAM_NAME, TEST_PARAM_VALUE);
        action.setMapParameters(params);
        return action;
    }

    private static ProcessorRequestForTests buildTestRequest(Action action) {
        ProcessorRequestForTests p = new ProcessorRequestForTests();
        if (action instanceof Action) {
            p.setAction(action);
        }
        return p;
    }
}
