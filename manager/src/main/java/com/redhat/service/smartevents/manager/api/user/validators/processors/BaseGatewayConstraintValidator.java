package com.redhat.service.smartevents.manager.api.user.validators.processors;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.hibernate.validator.internal.engine.messageinterpolation.util.InterpolationHelper;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.GatewayProviderException;
import com.redhat.service.smartevents.infra.models.gateways.Gateway;
import com.redhat.service.smartevents.infra.validations.ValidationResult;
import com.redhat.service.smartevents.processor.GatewayConfigurator;
import com.redhat.service.smartevents.processor.GatewayValidator;

abstract class BaseGatewayConstraintValidator<A extends Annotation, T> implements ConstraintValidator<A, T> {

    static final String GATEWAY_TYPE_MISSING_ERROR = "{gatewayClass} type must be specified";
    static final String GATEWAY_TYPE_NOT_RECOGNISED_ERROR = "{gatewayClass} of type '{type}' is not recognised.";
    static final String GATEWAY_PARAMETERS_MISSING_ERROR = "{gatewayClass} parameters must be supplied";
    static final String GATEWAY_PARAMETERS_NOT_VALID_ERROR = "Parameters for {gatewayClass} '{name}' of type '{type}' are not valid.";

    static final String GATEWAY_CLASS_PARAM = "gatewayClass";
    static final String TYPE_PARAM = "type";

    GatewayConfigurator gatewayConfigurator;

    BaseGatewayConstraintValidator() {
        //CDI proxy
    }

    BaseGatewayConstraintValidator(GatewayConfigurator gatewayConfigurator) {
        this.gatewayConfigurator = gatewayConfigurator;
    }

    protected <T extends Gateway> boolean isValidGateway(T gateway, ConstraintValidatorContext context, Function<String, GatewayValidator<T>> validatorGetter) {
        if (gateway.getType() == null) {
            addConstraintViolation(context, GATEWAY_TYPE_MISSING_ERROR,
                    Collections.singletonMap(GATEWAY_CLASS_PARAM, gateway.getClass().getSimpleName()));
            return false;
        }

        if (gateway.getParameters() == null) {
            addConstraintViolation(context, GATEWAY_PARAMETERS_MISSING_ERROR,
                    Collections.singletonMap(GATEWAY_CLASS_PARAM, gateway.getClass().getSimpleName()));
            return false;
        }

        GatewayValidator<T> validator;
        try {
            validator = validatorGetter.apply(gateway.getType());
        } catch (GatewayProviderException e) {
            addConstraintViolation(context, GATEWAY_TYPE_NOT_RECOGNISED_ERROR,
                    Map.of(GATEWAY_CLASS_PARAM, gateway.getClass().getSimpleName(), TYPE_PARAM, gateway.getType()));
            return false;
        }

        ValidationResult v = validator.isValid(gateway);

        if (!v.isValid()) {
            String message = v.getMessage();
            if (message == null) {
                addConstraintViolation(context, GATEWAY_PARAMETERS_NOT_VALID_ERROR,
                        Map.of(GATEWAY_CLASS_PARAM, gateway.getClass().getSimpleName(), TYPE_PARAM, gateway.getType()));
            } else {
                addConstraintViolation(context, InterpolationHelper.escapeMessageParameter(message), Collections.emptyMap());
            }
        }

        return v.isValid();
    }

    protected static void addConstraintViolation(ConstraintValidatorContext context, String message, Map<String, Object> messageParams) {
        context.disableDefaultConstraintViolation();
        if (!messageParams.isEmpty()) {
            HibernateConstraintValidatorContext hibernateContext = context.unwrap(HibernateConstraintValidatorContext.class);
            messageParams.forEach(hibernateContext::addMessageParameter);
        }
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
