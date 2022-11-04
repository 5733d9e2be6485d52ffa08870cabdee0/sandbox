package com.redhat.service.smartevents.manager.api.v1.user.validators.processors;

import java.lang.annotation.Annotation;
import java.util.Collections;

import javax.validation.ConstraintValidatorContext;

import org.hibernate.validator.internal.engine.messageinterpolation.util.InterpolationHelper;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.ProcessorGatewayParametersMissingException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ProcessorGatewayUnclassifiedException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ProcessorMissingGatewayException;
import com.redhat.service.smartevents.infra.models.gateways.Gateway;
import com.redhat.service.smartevents.infra.validations.ValidationResult;
import com.redhat.service.smartevents.processor.GatewayConfigurator;
import com.redhat.service.smartevents.processor.validators.GatewayValidator;

abstract class BaseGatewayConstraintValidator<A extends Annotation, T> extends BaseConstraintValidator<A, T> {

    static final String GATEWAY_TYPE_MISSING_ERROR = "{gatewayClass} type must be specified";
    static final String GATEWAY_PARAMETERS_MISSING_ERROR = "{gatewayClass} parameters must be supplied";

    static final String GATEWAY_CLASS_PARAM = "gatewayClass";
    static final String TYPE_PARAM = "type";

    GatewayConfigurator gatewayConfigurator;

    BaseGatewayConstraintValidator() {
        //CDI proxy
    }

    BaseGatewayConstraintValidator(GatewayConfigurator gatewayConfigurator) {
        this.gatewayConfigurator = gatewayConfigurator;
    }

    protected boolean isValidGateway(Gateway gateway, ConstraintValidatorContext context) {
        if (gateway.getType() == null) {
            addConstraintViolation(context, GATEWAY_TYPE_MISSING_ERROR,
                    Collections.singletonMap(GATEWAY_CLASS_PARAM, gateway.getClass().getSimpleName()),
                    ProcessorMissingGatewayException::new);
            return false;
        }

        if (gateway.getParameters() == null) {
            addConstraintViolation(context, GATEWAY_PARAMETERS_MISSING_ERROR,
                    Collections.singletonMap(GATEWAY_CLASS_PARAM, gateway.getClass().getSimpleName()),
                    ProcessorGatewayParametersMissingException::new);
            return false;
        }

        GatewayValidator validator = gatewayConfigurator.getValidator(gateway.getType());
        ValidationResult v = validator.isValid(gateway);

        if (!v.isValid()) {
            for (ValidationResult.Violation violation : v.getViolations()) {
                addConstraintViolation(context,
                        InterpolationHelper.escapeMessageParameter(violation.getException().getMessage()),
                        Collections.emptyMap(),
                        ProcessorGatewayUnclassifiedException::new);
            }
        }

        return v.isValid();
    }
}
