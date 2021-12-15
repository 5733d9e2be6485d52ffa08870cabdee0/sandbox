package com.redhat.service.bridge.manager.api.user.validators.actions;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.redhat.service.bridge.actions.ActionProvider;
import com.redhat.service.bridge.actions.ActionProviderFactory;
import com.redhat.service.bridge.actions.ValidationResult;
import com.redhat.service.bridge.infra.exceptions.definitions.ActionProviderException;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.manager.api.models.requests.ProcessorRequest;

@ApplicationScoped
public class ActionParamValidatorContainer implements ConstraintValidator<ValidActionParams, ProcessorRequest> {

    @Inject
    ActionProviderFactory actionProviderFactory;

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

        ActionProvider actionProvider;
        try {
            actionProvider = actionProviderFactory.getActionProvider(baseAction.getType());
        } catch (ActionProviderException e) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Action of type '" + baseAction.getType() + "' is not recognised.").addConstraintViolation();
            return false;
        }

        ValidationResult v = actionProvider.getParameterValidator().isValid(baseAction);

        if (!v.isValid()) {
            String message = v.getMessage();
            if (message == null) {
                message = "Parameters for Action '" + baseAction.getName() + "' of Type '" + baseAction.getType() + "' are not valid.";
            }
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        }

        return v.isValid();
    }
}
