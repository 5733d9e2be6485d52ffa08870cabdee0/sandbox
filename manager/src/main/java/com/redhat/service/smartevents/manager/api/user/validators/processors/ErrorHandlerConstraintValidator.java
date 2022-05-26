package com.redhat.service.smartevents.manager.api.user.validators.processors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ConstraintValidatorContext;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.manager.api.models.requests.BridgeRequest;
import com.redhat.service.smartevents.processor.GatewayConfigurator;

@ApplicationScoped
public class ErrorHandlerConstraintValidator extends BaseGatewayConstraintValidator<ValidErrorHandler, BridgeRequest> {

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

        return isValidGateway(action, context, gatewayConfigurator::getActionValidator);
    }

}
