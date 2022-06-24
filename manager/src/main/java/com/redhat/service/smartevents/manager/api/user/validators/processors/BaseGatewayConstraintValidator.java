package com.redhat.service.smartevents.manager.api.user.validators.processors;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.MessageInterpolator;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.hibernate.validator.internal.engine.MessageInterpolatorContext;
import org.hibernate.validator.internal.engine.messageinterpolation.util.InterpolationHelper;
import org.hibernate.validator.messageinterpolation.ExpressionLanguageFeatureLevel;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.ExternalUserException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ProcessorGatewayParametersMissingException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ProcessorGatewayUnclassifiedException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ProcessorMissingGatewayException;
import com.redhat.service.smartevents.infra.models.gateways.Gateway;
import com.redhat.service.smartevents.infra.validations.ValidationResult;
import com.redhat.service.smartevents.processor.GatewayConfigurator;
import com.redhat.service.smartevents.processor.validators.GatewayValidator;

abstract class BaseGatewayConstraintValidator<A extends Annotation, T> implements ConstraintValidator<A, T> {

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

    protected static void addConstraintViolation(ConstraintValidatorContext context,
            String message,
            Map<String, Object> messageParams,
            Function<String, ExternalUserException> userExceptionSupplier) {
        context.disableDefaultConstraintViolation();
        HibernateConstraintValidatorContext hibernateContext = context.unwrap(HibernateConstraintValidatorContext.class);
        hibernateContext.withDynamicPayload(userExceptionSupplier.apply(interpolateMessage(message, messageParams)));

        if (!messageParams.isEmpty()) {
            messageParams.forEach(hibernateContext::addMessageParameter);
        }
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }

    private static String interpolateMessage(String message, Map<String, Object> messageParams) {
        // Use a minimal Message Interpolator for our purposes.
        // We only use a template and parameters; so it is (reasonably) safe to pass nulls, but put in a try {..} catch() block to be sure.
        // Tests, both programmatic, and real did not reveal any issue but let's not tempt an RTE when live!
        MessageInterpolator interpolator = new ParameterMessageInterpolator();
        MessageInterpolatorContext ic = new MessageInterpolatorContext(null, null, null, null, messageParams, Collections.emptyMap(), ExpressionLanguageFeatureLevel.DEFAULT, false);
        try {
            return interpolator.interpolate(message, ic);
        } catch (Exception e) {
            return message;
        }
    }
}
