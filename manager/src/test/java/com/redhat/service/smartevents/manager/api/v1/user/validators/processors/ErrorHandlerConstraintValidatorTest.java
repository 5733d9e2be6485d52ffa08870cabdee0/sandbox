package com.redhat.service.smartevents.manager.api.v1.user.validators.processors;

import java.util.Collections;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.hibernate.validator.constraintvalidation.HibernateConstraintViolationBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.infra.validations.ValidationResult;
import com.redhat.service.smartevents.manager.TestConstants;
import com.redhat.service.smartevents.manager.api.v1.models.requests.BridgeRequest;
import com.redhat.service.smartevents.processor.GatewayConfigurator;
import com.redhat.service.smartevents.processor.actions.aws.AwsLambdaAction;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;
import com.redhat.service.smartevents.processor.validators.GatewayValidator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ErrorHandlerConstraintValidatorTest {

    ErrorHandlerConstraintValidator errorHandlerConstraintValidator;

    @Mock
    GatewayConfigurator gatewayConfiguratorMock;
    @Mock
    GatewayValidator validatorMock;
    @Mock
    HibernateConstraintValidatorContext validatorContextMock;
    @Mock
    HibernateConstraintViolationBuilder builderMock;

    @BeforeEach
    public void beforeEach() {
        lenient().when(validatorMock.isValid(any(Action.class))).thenReturn(ValidationResult.valid());
        lenient().when(validatorMock.isValid(any(Source.class))).thenReturn(ValidationResult.valid());

        lenient().when(gatewayConfiguratorMock.getValidator(any(String.class))).thenReturn(validatorMock);

        lenient().when(validatorContextMock.buildConstraintViolationWithTemplate(any(String.class))).thenReturn(builderMock);
        lenient().when(validatorContextMock.unwrap(HibernateConstraintValidatorContext.class)).thenReturn(validatorContextMock);

        errorHandlerConstraintValidator = new ErrorHandlerConstraintValidator(gatewayConfiguratorMock);
    }

    @Test
    public void nullErrorHandlerIsValid() {
        BridgeRequest bridgeRequest = new BridgeRequest("bridge", TestConstants.DEFAULT_CLOUD_PROVIDER, TestConstants.DEFAULT_REGION, null);
        assertThat(errorHandlerConstraintValidator.isValid(bridgeRequest, validatorContextMock)).isTrue();
    }

    @Test
    public void webhookActionIsValid() {
        Action action = new Action();
        action.setType(WebhookAction.TYPE);
        action.setMapParameters(Collections.emptyMap());
        BridgeRequest bridgeRequest = new BridgeRequest("bridge", TestConstants.DEFAULT_CLOUD_PROVIDER, TestConstants.DEFAULT_REGION, action);
        assertThat(errorHandlerConstraintValidator.isValid(bridgeRequest, validatorContextMock)).isTrue();
    }

    @Test
    public void kafkaActionIsValid() {
        Action action = new Action();
        action.setType(KafkaTopicAction.TYPE);
        action.setMapParameters(Collections.emptyMap());
        BridgeRequest bridgeRequest = new BridgeRequest("bridge", TestConstants.DEFAULT_CLOUD_PROVIDER, TestConstants.DEFAULT_REGION, action);
        assertThat(errorHandlerConstraintValidator.isValid(bridgeRequest, validatorContextMock)).isTrue();
    }

    @Test
    public void awsLambdaActionIsInvalid() {
        Action action = new Action();
        action.setType(AwsLambdaAction.TYPE);
        action.setMapParameters(Collections.emptyMap());
        BridgeRequest bridgeRequest = new BridgeRequest("bridge", TestConstants.DEFAULT_CLOUD_PROVIDER, TestConstants.DEFAULT_REGION, action);
        assertThat(errorHandlerConstraintValidator.isValid(bridgeRequest, validatorContextMock)).isFalse();
    }

}
