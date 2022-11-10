package com.redhat.service.smartevents.manager.v1.api.validators.processors;

import java.lang.annotation.Annotation;
import java.util.Collections;

import javax.validation.ConstraintValidatorContext;

import org.hibernate.validator.internal.engine.messageinterpolation.util.InterpolationHelper;

import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.ProcessorGatewayParametersMissingException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.ProcessorGatewayUnclassifiedException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.ProcessorMissingGatewayException;
import com.redhat.service.smartevents.infra.core.validations.ValidationResult;
import com.redhat.service.smartevents.infra.v1.api.models.gateways.Gateway;
import com.redhat.service.smartevents.manager.core.api.validators.BaseConstraintValidator;
import com.redhat.service.smartevents.processor.GatewayConfigurator;
import com.redhat.service.smartevents.processor.validators.GatewayValidator;

public abstract class BaseGatewayConstraintValidator<A extends Annotation, T> extends BaseConstraintValidator<A, T> {

    public static final String GATEWAY_TYPE_MISSING_ERROR = "{gatewayClass} type must be specified";
    public static final String GATEWAY_PARAMETERS_MISSING_ERROR = "{gatewayClass} parameters must be supplied";

    public static final String GATEWAY_CLASS_PARAM = "gatewayClass";
    public static final String TYPE_PARAM = "type";

    GatewayConfigurator gatewayConfigurator;

    protected BaseGatewayConstraintValidator() {
        //CDI proxy
    }

    protected BaseGatewayConstraintValidator(GatewayConfigurator gatewayConfigurator) {
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
