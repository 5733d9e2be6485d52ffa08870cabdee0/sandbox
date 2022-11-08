package com.redhat.service.smartevents.manager.v1.api.user.validators.processors;

import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ConstraintValidatorContext;

import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.UnsupportedErrorHandlerGatewayException;
import com.redhat.service.smartevents.infra.v1.api.models.gateways.Action;
import com.redhat.service.smartevents.manager.v1.api.models.requests.BridgeRequest;
import com.redhat.service.smartevents.manager.v1.api.validators.processors.BaseGatewayConstraintValidator;
import com.redhat.service.smartevents.manager.v1.services.ProcessingErrorService;
import com.redhat.service.smartevents.processor.GatewayConfigurator;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;

@ApplicationScoped
public class ErrorHandlerConstraintValidator extends BaseGatewayConstraintValidator<ValidErrorHandler, BridgeRequest> {

    static final String UNSUPPORTED_ERROR_HANDLER_TYPE_ERROR = "Only error handlers of type " +
            "\"" + ProcessingErrorService.ENDPOINT_ERROR_HANDLER_TYPE + "\", " +
            "\"" + KafkaTopicAction.TYPE + "\" and \"" + WebhookAction.TYPE + "\" are supported";

    public ErrorHandlerConstraintValidator() {
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

        if (ProcessingErrorService.isEndpointErrorHandlerAction(errorHandlerAction)) {
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
