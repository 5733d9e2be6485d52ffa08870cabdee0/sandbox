package com.redhat.service.rhose.manager.api.user.validators.actions;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.hibernate.validator.internal.engine.messageinterpolation.util.InterpolationHelper;

import com.redhat.service.rhose.infra.exceptions.definitions.user.ActionProviderException;
import com.redhat.service.rhose.infra.models.actions.BaseAction;
import com.redhat.service.rhose.infra.validations.ValidationResult;
import com.redhat.service.rhose.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.rhose.processor.actions.ActionConfigurator;
import com.redhat.service.rhose.processor.actions.ActionValidator;

@ApplicationScoped
public class ActionParamValidatorContainer implements ConstraintValidator<ValidActionParams, ProcessorRequest> {

    static final String ACTION_TYPE_NOT_RECOGNISED_ERROR = "Action of type '{type}' is not recognised.";

    static final String ACTION_PARAMETERS_NOT_VALID_ERROR = "Parameters for Action '{name}' of Type '{type}' are not valid.";

    static final String TYPE_PARAM = "type";

    @Inject
    ActionConfigurator actionConfigurator;

    @Override
    public boolean isValid(ProcessorRequest value, ConstraintValidatorContext context) {

        /*
         * Centralised handling of Action parameters. The idea here being that for each Action we support, we
         * provide the ability to check:
         *
         * - The action 'type' is recognised
         * - The parameters supplied to configure the Action are valid.
         */

        BaseAction baseAction = value.getAction();
        if (baseAction == null) {
            return false;
        }

        if (baseAction.getParameters() == null) {
            return false;
        }

        ActionValidator actionValidator;
        try {
            actionValidator = actionConfigurator.getValidator(baseAction.getType());
        } catch (ActionProviderException e) {
            context.disableDefaultConstraintViolation();
            HibernateConstraintValidatorContext hibernateContext = context.unwrap(HibernateConstraintValidatorContext.class);
            hibernateContext.addMessageParameter(TYPE_PARAM, baseAction.getType());
            hibernateContext.buildConstraintViolationWithTemplate(ACTION_TYPE_NOT_RECOGNISED_ERROR).addConstraintViolation();
            return false;
        }

        ValidationResult v = actionValidator.isValid(baseAction);

        if (!v.isValid()) {
            String message = v.getMessage();
            context.disableDefaultConstraintViolation();
            if (message == null) {
                message = ACTION_PARAMETERS_NOT_VALID_ERROR;
                HibernateConstraintValidatorContext hibernateContext = context.unwrap(HibernateConstraintValidatorContext.class);
                hibernateContext.addMessageParameter(TYPE_PARAM, baseAction.getType());
            } else {
                message = InterpolationHelper.escapeMessageParameter(message);
            }

            context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        }

        return v.isValid();
    }
}
