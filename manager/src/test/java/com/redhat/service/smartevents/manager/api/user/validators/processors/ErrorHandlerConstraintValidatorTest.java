package com.redhat.service.smartevents.manager.api.user.validators.processors;

import java.util.Map;
import java.util.function.Function;

import javax.validation.ConstraintValidatorContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.ExternalUserException;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Gateway;
import com.redhat.service.smartevents.manager.api.models.requests.BridgeRequest;
import com.redhat.service.smartevents.processor.actions.aws.AwsLambdaAction;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorHandlerConstraintValidatorTest {

    ErrorHandlerConstraintValidator errorHandlerConstraintValidator;

    @BeforeEach
    public void init() {
        errorHandlerConstraintValidator = new ErrorHandlerConstraintValidator() {
            @Override
            protected boolean isValidGateway(Gateway gateway, ConstraintValidatorContext context) {
                return true;
            }

            @Override
            protected void addConstraintViolation(ConstraintValidatorContext context, String message, Map<String, Object> messageParams,
                    Function<String, ExternalUserException> userExceptionSupplier) {
            }
        };
    }

    @Test
    public void nullErrorHandlerIsValid() {
        BridgeRequest bridgeRequest = new BridgeRequest("bridge", null);
        assertThat(errorHandlerConstraintValidator.isValid(bridgeRequest, null)).isTrue();
    }

    @Test
    public void webhookActionIsValid() {
        Action action = new Action();
        action.setType(WebhookAction.TYPE);
        BridgeRequest bridgeRequest = new BridgeRequest("bridge", action);
        assertThat(errorHandlerConstraintValidator.isValid(bridgeRequest, null)).isTrue();
    }

    @Test
    public void kafkaActionIsValid() {
        Action action = new Action();
        action.setType(KafkaTopicAction.TYPE);
        BridgeRequest bridgeRequest = new BridgeRequest("bridge", action);
        assertThat(errorHandlerConstraintValidator.isValid(bridgeRequest, null)).isTrue();
    }

    @Test
    public void awsLambdaActionIsInvalid() {
        Action action = new Action();
        action.setType(AwsLambdaAction.TYPE);
        BridgeRequest bridgeRequest = new BridgeRequest("bridge", action);
        assertThat(errorHandlerConstraintValidator.isValid(bridgeRequest, null)).isFalse();
    }

}
