package com.redhat.service.smartevents.manager.api.user.validators.processors;

import java.util.Collections;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ConstraintValidatorContext;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.UnsupportedErrorHandlerGatewayException;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.manager.api.models.requests.BridgeRequest;
import com.redhat.service.smartevents.processor.GatewayConfigurator;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;

@ApplicationScoped
public class ErrorHandlerConstraintValidator extends BaseGatewayConstraintValidator<ValidErrorHandler, BridgeRequest> {

    static final String UNSUPPORTED_ERROR_HANDLER_TYPE_ERROR = "Only error handlers of type \"" + WebhookAction.TYPE + "\" are supported";

    protected ErrorHandlerConstraintValidator() {
        //CDI proxy
    }

    @Inject
    public ErrorHandlerConstraintValidator(GatewayConfigurator gatewayConfigurator, GatewayConfigurator gatewayConfigurator1) {
        super(gatewayConfigurator);
        this.gatewayConfigurator = gatewayConfigurator1;
    }

    @Override
    public boolean isValid(BridgeRequest value, ConstraintValidatorContext context) {
        Action action = value.getErrorHandler();

        if (action == null) {
            return true;
        }

        if (!isValidGateway(action, context)) {
            return false;
        }

        if (!Objects.equals(action.getType(), WebhookAction.TYPE)) {
            addConstraintViolation(context,
                    UNSUPPORTED_ERROR_HANDLER_TYPE_ERROR,
                    Collections.emptyMap(),
                    UnsupportedErrorHandlerGatewayException::new);
            return false;
        }

        return true;
    }

}
