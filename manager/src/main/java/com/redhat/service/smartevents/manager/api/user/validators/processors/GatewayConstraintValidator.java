package com.redhat.service.smartevents.manager.api.user.validators.processors;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.hibernate.validator.internal.engine.messageinterpolation.util.InterpolationHelper;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.GatewayProviderException;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Gateway;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;
import com.redhat.service.smartevents.infra.validations.ValidationResult;
import com.redhat.service.smartevents.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.processor.AbstractGatewayValidator;
import com.redhat.service.smartevents.processor.GatewayConfigurator;

@ApplicationScoped
public class GatewayConstraintValidator implements ConstraintValidator<ValidGateway, ProcessorRequest> {

    static final String MISSING_GATEWAY_ERROR = "Processor must have either \"action\" or \"source\"";
    static final String MULTIPLE_GATEWAY_ERROR = "Processor can't have both \"action\" and \"source\"";
    static final String SOURCE_PROCESSOR_WITH_TRANSFORMATION_ERROR = "Source processors don't support transformations";
    static final String GATEWAY_TYPE_MISSING_ERROR = "{gatewayClass} type must be specified";
    static final String GATEWAY_TYPE_NOT_RECOGNISED_ERROR = "{gatewayClass} of type '{type}' is not recognised.";
    static final String GATEWAY_PARAMETERS_MISSING_ERROR = "{gatewayClass} parameters must be supplied";
    static final String GATEWAY_PARAMETERS_NOT_VALID_ERROR = "Parameters for {gatewayClass} '{name}' of type '{type}' are not valid.";

    static final String GATEWAY_CLASS_PARAM = "gatewayClass";
    static final String TYPE_PARAM = "type";

    @Inject
    GatewayConfigurator gatewayConfigurator;

    @Override
    public boolean isValid(ProcessorRequest value, ConstraintValidatorContext context) {
        Action action = value.getAction();
        Source source = value.getSource();

        if (action == null && source == null) {
            addConstraintViolation(context, MISSING_GATEWAY_ERROR, Collections.emptyMap());
            return false;
        }
        if (action != null && source != null) {
            addConstraintViolation(context, MULTIPLE_GATEWAY_ERROR, Collections.emptyMap());
            return false;
        }

        // currently source processors don't support transformation
        if (value.getType() == ProcessorType.SOURCE && value.getTransformationTemplate() != null) {
            addConstraintViolation(context, SOURCE_PROCESSOR_WITH_TRANSFORMATION_ERROR, Collections.emptyMap());
            return false;
        }

        return action != null
                ? isValidGateway(action, context, gatewayConfigurator::getActionValidator)
                : isValidGateway(source, context, gatewayConfigurator::getSourceValidator);
    }

    private <T extends Gateway> boolean isValidGateway(T gateway, ConstraintValidatorContext context, Function<String, AbstractGatewayValidator<T>> validatorGetter) {
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

        AbstractGatewayValidator<T> validator;
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

    private static void addConstraintViolation(ConstraintValidatorContext context, String message, Map<String, Object> messageParams) {
        context.disableDefaultConstraintViolation();
        if (!messageParams.isEmpty()) {
            HibernateConstraintValidatorContext hibernateContext = context.unwrap(HibernateConstraintValidatorContext.class);
            messageParams.forEach(hibernateContext::addMessageParameter);
        }
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
