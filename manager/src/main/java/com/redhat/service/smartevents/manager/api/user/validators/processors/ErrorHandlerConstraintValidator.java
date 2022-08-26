package com.redhat.service.smartevents.manager.api.user.validators.processors;

import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ConstraintValidatorContext;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.UnsupportedErrorHandlerGatewayException;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.manager.api.models.requests.BridgeRequest;
import com.redhat.service.smartevents.processor.GatewayConfigurator;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;

import static com.redhat.service.smartevents.manager.BridgesService.ENDPOINT_ERROR_HANDLER_TYPE;

@ApplicationScoped
public class ErrorHandlerConstraintValidator extends BaseGatewayConstraintValidator<ValidErrorHandler, BridgeRequest> {

    static final String UNSUPPORTED_ERROR_HANDLER_TYPE_ERROR = "Only error handlers of type \"" + ENDPOINT_ERROR_HANDLER_TYPE + "\", " +
            "\"" + KafkaTopicAction.TYPE + "\" and \"" + WebhookAction.TYPE + "\" are supported";

    protected ErrorHandlerConstraintValidator() {
        //CDI proxy
    }

    @Inject
    public ErrorHandlerConstraintValidator(GatewayConfigurator gatewayConfigurator) {
        super(gatewayConfigurator);
    }

    @Override
    public boolean isValid(BridgeRequest bridgeRequest, ConstraintValidatorContext context) {
        Action errorHandlerAction = bridgeRequest.getErrorHandler();

        if (errorHandlerAction == null) {
            return true;
        }

        if (ENDPOINT_ERROR_HANDLER_TYPE.equals(errorHandlerAction.getType())) {
            return true;
        }

        if (!isValidGateway(errorHandlerAction, context)) {
            return false;
        }

        if (!List.of(WebhookAction.TYPE, KafkaTopicAction.TYPE).contains(errorHandlerAction.getType())) {
            addConstraintViolation(context,
                    UNSUPPORTED_ERROR_HANDLER_TYPE_ERROR,
                    Collections.emptyMap(),
                    UnsupportedErrorHandlerGatewayException::new);
            return false;
        }

        return true;
    }

}
