package com.redhat.service.smartevents.manager.api.user.validators.processors;

import java.util.Collections;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.hibernate.validator.internal.engine.messageinterpolation.util.InterpolationHelper;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.ActionProviderException;
import com.redhat.service.smartevents.infra.models.actions.Action;
import com.redhat.service.smartevents.infra.models.actions.Source;
import com.redhat.service.smartevents.infra.validations.ValidationResult;
import com.redhat.service.smartevents.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.processor.actions.ActionConfigurator;
import com.redhat.service.smartevents.processor.actions.ActionValidator;

@ApplicationScoped
public class GatewayConstraintValidator implements ConstraintValidator<ValidGateway, ProcessorRequest> {

    static final String ACTION_TYPE_MISSING_ERROR = "Action type must be specified";
    static final String ACTION_TYPE_NOT_RECOGNISED_ERROR = "Action of type '{type}' is not recognised.";
    static final String ACTION_PARAMETERS_MISSING_ERROR = "Action parameters must be supplied";
    static final String ACTION_PARAMETERS_NOT_VALID_ERROR = "Parameters for action '{name}' of type '{type}' are not valid.";

    static final String SOURCE_TYPE_MISSING_ERROR = "Source type must be specified";
    static final String SOURCE_TYPE_NOT_RECOGNISED_ERROR = "Source of type '{type}' is not recognised.";
    static final String SOURCE_PARAMETERS_MISSING_ERROR = "Source parameters must be supplied";
    static final String SOURCE_PARAMETERS_NOT_VALID_ERROR = "Parameters for source '{name}' of type '{type}' are not valid.";

    static final String TYPE_PARAM = "type";

    @Inject
    ActionConfigurator actionConfigurator;

    @Override
    public boolean isValid(ProcessorRequest value, ConstraintValidatorContext context) {
        Action action = value.getAction();
        Source source = value.getSource();

        if (action == null && source == null || action != null && source != null) {
            return false;
        }

        return action != null ? isValidAction(action, context) : isValidSource(source, context);
    }

    private boolean isValidAction(Action action, ConstraintValidatorContext context) {
        if (action.getType() == null) {
            addConstraintViolation(context, ACTION_TYPE_MISSING_ERROR, Collections.emptyMap());
            return false;
        }

        if (action.getParameters() == null) {
            addConstraintViolation(context, ACTION_PARAMETERS_MISSING_ERROR, Collections.emptyMap());
            return false;
        }

        ActionValidator actionValidator;
        try {
            actionValidator = actionConfigurator.getValidator(action.getType());
        } catch (ActionProviderException e) {
            addConstraintViolation(context, ACTION_TYPE_NOT_RECOGNISED_ERROR, Map.of(TYPE_PARAM, action.getType()));
            return false;
        }

        ValidationResult v = actionValidator.isValid(action);

        if (!v.isValid()) {
            String message = v.getMessage();
            if (message == null) {
                addConstraintViolation(context, ACTION_PARAMETERS_NOT_VALID_ERROR, Map.of(TYPE_PARAM, action.getType()));
            } else {
                addConstraintViolation(context, InterpolationHelper.escapeMessageParameter(message), Collections.emptyMap());
            }
        }

        return v.isValid();
    }

    private boolean isValidSource(Source source, ConstraintValidatorContext context) {
        if (source.getType() == null) {
            addConstraintViolation(context, SOURCE_TYPE_MISSING_ERROR, Collections.emptyMap());
            return false;
        }

        if (source.getParameters() == null) {
            addConstraintViolation(context, SOURCE_PARAMETERS_MISSING_ERROR, Collections.emptyMap());
            return false;
        }

        return true;
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
