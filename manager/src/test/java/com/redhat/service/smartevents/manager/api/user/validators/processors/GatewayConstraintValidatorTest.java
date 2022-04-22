package com.redhat.service.smartevents.manager.api.user.validators.processors;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.hibernate.validator.constraintvalidation.HibernateConstraintViolationBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.verification.VerificationMode;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.GatewayProviderException;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Gateway;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.infra.validations.ValidationResult;
import com.redhat.service.smartevents.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.processor.GatewayConfigurator;
import com.redhat.service.smartevents.processor.GatewayValidator;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static com.redhat.service.smartevents.manager.api.user.validators.processors.GatewayConstraintValidator.GATEWAY_CLASS_PARAM;
import static com.redhat.service.smartevents.manager.api.user.validators.processors.GatewayConstraintValidator.GATEWAY_PARAMETERS_MISSING_ERROR;
import static com.redhat.service.smartevents.manager.api.user.validators.processors.GatewayConstraintValidator.GATEWAY_PARAMETERS_NOT_VALID_ERROR;
import static com.redhat.service.smartevents.manager.api.user.validators.processors.GatewayConstraintValidator.GATEWAY_TYPE_MISSING_ERROR;
import static com.redhat.service.smartevents.manager.api.user.validators.processors.GatewayConstraintValidator.GATEWAY_TYPE_NOT_RECOGNISED_ERROR;
import static com.redhat.service.smartevents.manager.api.user.validators.processors.GatewayConstraintValidator.MISSING_GATEWAY_ERROR;
import static com.redhat.service.smartevents.manager.api.user.validators.processors.GatewayConstraintValidator.MULTIPLE_GATEWAY_ERROR;
import static com.redhat.service.smartevents.manager.api.user.validators.processors.GatewayConstraintValidator.TYPE_PARAM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class GatewayConstraintValidatorTest {

    public static String TEST_ACTION_TYPE = "TestAction";
    public static String TEST_SOURCE_TYPE = "TestSource";
    public static String TEST_PARAM_NAME = "test-param-name";
    public static String TEST_PARAM_VALUE = "test-param-value";

    @Inject
    GatewayConstraintValidator container;

    @InjectMock
    GatewayConfigurator gatewayConfiguratorMock;

    GatewayValidator<Action> actionValidatorMock;

    GatewayValidator<Source> sourceValidatorMock;

    HibernateConstraintValidatorContext validatorContextMock;

    HibernateConstraintViolationBuilder builderMock;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void beforeEach() {
        actionValidatorMock = mock(GatewayValidator.class);
        when(actionValidatorMock.isValid(any())).thenReturn(ValidationResult.valid());

        sourceValidatorMock = mock(GatewayValidator.class);
        when(sourceValidatorMock.isValid(any())).thenReturn(ValidationResult.valid());

        reset(gatewayConfiguratorMock);
        when(gatewayConfiguratorMock.getActionValidator(TEST_ACTION_TYPE)).thenReturn(actionValidatorMock);
        when(gatewayConfiguratorMock.getActionValidator(not(eq(TEST_ACTION_TYPE)))).thenThrow(new GatewayProviderException("No action provider found"));
        when(gatewayConfiguratorMock.getSourceValidator(TEST_SOURCE_TYPE)).thenReturn(sourceValidatorMock);
        when(gatewayConfiguratorMock.getSourceValidator(not(eq(TEST_SOURCE_TYPE)))).thenThrow(new GatewayProviderException("No source provider found"));

        validatorContextMock = mock(HibernateConstraintValidatorContext.class);
        builderMock = mock(HibernateConstraintViolationBuilder.class);
        when(validatorContextMock.buildConstraintViolationWithTemplate(any(String.class))).thenReturn(builderMock);
        when(validatorContextMock.unwrap(HibernateConstraintValidatorContext.class)).thenReturn(validatorContextMock);
    }

    @Test
    void isValid_noGatewaysIsNotValid() {
        ProcessorRequest p = new ProcessorRequest();

        assertThat(container.isValid(p, validatorContextMock)).isFalse();
        verify(gatewayConfiguratorMock, never()).getActionValidator(any());
        verify(gatewayConfiguratorMock, never()).getSourceValidator(any());
        verify(actionValidatorMock, never()).isValid(any());
        verify(sourceValidatorMock, never()).isValid(any());
        verifyErrorMessage(MISSING_GATEWAY_ERROR);
    }

    @Test
    void isValid_multipleGatewaysIsNotValid() {
        ProcessorRequest p = new ProcessorRequest();
        p.setAction(buildTestAction());
        p.setSource(buildTestSource());

        assertThat(container.isValid(p, validatorContextMock)).isFalse();
        verify(gatewayConfiguratorMock, never()).getActionValidator(any());
        verify(gatewayConfiguratorMock, never()).getSourceValidator(any());
        verify(actionValidatorMock, never()).isValid(any());
        verify(sourceValidatorMock, never()).isValid(any());
        verifyErrorMessage(MULTIPLE_GATEWAY_ERROR);
    }

    @ParameterizedTest
    @MethodSource("gateways")
    void isValid(Gateway gateway) {
        ProcessorRequest p = buildTestRequest(gateway);

        assertThat(container.isValid(p, validatorContextMock)).isTrue();
        verifyGetValidatorCall(gateway, times(1), gateway.getType());
        verifyIsValidCall(gateway, times(1));
    }

    @ParameterizedTest
    @MethodSource("gateways")
    void isValid_gatewayWithNullParametersIsNotValid(Gateway gateway) {
        gateway.setParameters(null);
        ProcessorRequest p = buildTestRequest(gateway);

        assertThat(container.isValid(p, validatorContextMock)).isFalse();
        verifyGetValidatorCall(gateway, never(), ArgumentMatchers::anyString);
        verifyIsValidCall(gateway, never());
        verifyErrorMessage(GATEWAY_PARAMETERS_MISSING_ERROR, gateway, false);
    }

    @ParameterizedTest
    @MethodSource("gateways")
    void isValid_gatewayWithEmptyParamsIsValid(Gateway gateway) {
        gateway.getParameters().clear();
        ProcessorRequest p = buildTestRequest(gateway);

        assertThat(container.isValid(p, validatorContextMock)).isTrue();
        verifyGetValidatorCall(gateway, times(1), gateway.getType());
        verifyIsValidCall(gateway, times(1));
    }

    @ParameterizedTest
    @MethodSource("gateways")
    void isValid_nullGatewayTypeIsNotValid(Gateway gateway) {
        gateway.setType(null);
        ProcessorRequest p = buildTestRequest(gateway);

        assertThat(container.isValid(p, validatorContextMock)).isFalse();
        verifyGetValidatorCall(gateway, never(), ArgumentMatchers::anyString);
        verifyIsValidCall(gateway, never());
        verifyErrorMessage(GATEWAY_TYPE_MISSING_ERROR, gateway, false);
    }

    @ParameterizedTest
    @MethodSource("gateways")
    void isValid_unrecognisedGatewayTypeIsNotValid(Gateway gateway) {
        String doesNotExistType = "doesNotExist";

        gateway.setType(doesNotExistType);
        ProcessorRequest p = buildTestRequest(gateway);

        assertThat(container.isValid(p, validatorContextMock)).isFalse();
        verifyGetValidatorCall(gateway, times(1), doesNotExistType);
        verifyIsValidCall(gateway, never());
        verifyErrorMessage(GATEWAY_TYPE_NOT_RECOGNISED_ERROR, gateway, true);
    }

    @ParameterizedTest
    @MethodSource("gateways")
    void isValid_messageFromGatewayValidatorAddedOnFailure(Gateway gateway) {
        String testErrorMessage = "This is a test error message returned from validator";
        when(actionValidatorMock.isValid(any())).thenReturn(ValidationResult.invalid(testErrorMessage));
        when(sourceValidatorMock.isValid(any())).thenReturn(ValidationResult.invalid(testErrorMessage));

        ProcessorRequest p = buildTestRequest(gateway);

        assertThat(container.isValid(p, validatorContextMock)).isFalse();
        verifyGetValidatorCall(gateway, times(1), gateway.getType());
        verifyIsValidCall(gateway, times(1));
        verifyErrorMessage(testErrorMessage);
    }

    @ParameterizedTest
    @MethodSource("gateways")
    void isValid_noMessageFromValidatorGenericMessageAdded(Gateway gateway) {
        when(actionValidatorMock.isValid(any())).thenReturn(ValidationResult.invalid());
        when(sourceValidatorMock.isValid(any())).thenReturn(ValidationResult.invalid());

        ProcessorRequest p = buildTestRequest(gateway);

        assertThat(container.isValid(p, validatorContextMock)).isFalse();
        verifyGetValidatorCall(gateway, times(1), gateway.getType());
        verifyIsValidCall(gateway, times(1));
        verifyErrorMessage(GATEWAY_PARAMETERS_NOT_VALID_ERROR, gateway, true);
    }

    private void verifyGetValidatorCall(Gateway gateway, VerificationMode mode, String argument) {
        verifyGetValidatorCall(gateway, mode, () -> argument);
    }

    private void verifyGetValidatorCall(Gateway gateway, VerificationMode mode, Supplier<String> argumentSupplier) {
        if (gateway instanceof Action) {
            verify(gatewayConfiguratorMock, mode).getActionValidator(argumentSupplier.get());
        } else {
            verify(gatewayConfiguratorMock, mode).getSourceValidator(argumentSupplier.get());
        }
    }

    private void verifyIsValidCall(Gateway gateway, VerificationMode mode) {
        if (gateway instanceof Action) {
            verify(actionValidatorMock, mode).isValid(any());
            verify(sourceValidatorMock, never()).isValid(any());
        } else {
            verify(sourceValidatorMock, mode).isValid(any());
            verify(actionValidatorMock, never()).isValid(any());
        }
    }

    private void verifyErrorMessage(String expectedMessage) {
        ArgumentCaptor<String> messageCap = ArgumentCaptor.forClass(String.class);

        verify(validatorContextMock).disableDefaultConstraintViolation();
        verify(validatorContextMock).buildConstraintViolationWithTemplate(messageCap.capture());
        verify(builderMock).addConstraintViolation();

        assertThat(messageCap.getValue()).isEqualTo(expectedMessage);
    }

    private void verifyErrorMessage(String expectedMessage, Gateway gateway, boolean verifyTypeParam) {
        verifyErrorMessage(expectedMessage);
        verify(validatorContextMock).addMessageParameter(GATEWAY_CLASS_PARAM, gateway.getClass().getSimpleName());
        if (verifyTypeParam) {
            verify(validatorContextMock).addMessageParameter(TYPE_PARAM, gateway.getType());
        }
    }

    private static Stream<Arguments> gateways() {
        return Stream.of(buildTestAction(), buildTestSource()).map(Arguments::of);
    }

    private static Action buildTestAction() {
        Action action = new Action();
        action.setType(TEST_ACTION_TYPE);
        Map<String, String> params = new HashMap<>();
        params.put(TEST_PARAM_NAME, TEST_PARAM_VALUE);
        action.setParameters(params);
        return action;
    }

    private static Source buildTestSource() {
        Source source = new Source();
        source.setType(TEST_SOURCE_TYPE);
        Map<String, String> params = new HashMap<>();
        params.put(TEST_PARAM_NAME, TEST_PARAM_VALUE);
        source.setParameters(params);
        return source;
    }

    private static ProcessorRequest buildTestRequest(Gateway gateway) {
        ProcessorRequest p = new ProcessorRequest();
        if (gateway instanceof Action) {
            p.setAction((Action) gateway);
        }
        if (gateway instanceof Source) {
            p.setSource((Source) gateway);
        }
        return p;
    }
}
