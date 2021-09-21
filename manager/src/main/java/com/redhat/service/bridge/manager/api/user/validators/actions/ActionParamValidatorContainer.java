package com.redhat.service.bridge.manager.api.user.validators.actions;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.manager.api.models.requests.ProcessorRequest;

@ApplicationScoped
public class ActionParamValidatorContainer implements ConstraintValidator<ValidActionParams, ProcessorRequest> {

    @Inject
    Instance<ActionParamsValidator> validators;

    @Override
    public boolean isValid(ProcessorRequest value, ConstraintValidatorContext context) {

        /*
         * Centralised handling of Action parameters. The idea here being that for each Action we support, we
         * provide the ability to check:
         * 
         * - The action 'type' is recognised
         * - The parameters supplied to configure the Action are valid.
         * 
         * This is OK for our initial work on Actions, but as the number of Actions increases, it feels natural that
         * we'd want to offload the types and list of required parameters for Actions to a schema registry. We can then provide a generic
         * validation implementation that fetches the parameter schema from a Registry and checks submitted values are OK.
         * 
         * That also implies versioning of Actions e.g. v1 supports only 'topic' parameter, but v2 supports 'topic' and 'sender' parameter.
         * TBD.
         */

        BaseAction baseAction = value.getAction();
        if (baseAction == null) {
            return false;
        }

        Optional<ActionParamsValidator> paramsValidator = validators.stream()
                .filter((v) -> v.accepts(baseAction))
                .findFirst();

        if (paramsValidator.isPresent()) {
            boolean valid = paramsValidator.get().isValid(baseAction);

            if (!valid) {
                context.buildConstraintViolationWithTemplate("Parameters for Action '" + baseAction.getName() + "' of Type '" + baseAction.getType() + "' are not valid");
            }

            return valid;
        } else {
            // No validator present for the submitted "type" of Action. It's not something that we recognise/support
            context.buildConstraintViolationWithTemplate("Action of type '" + baseAction.getType() + "' is not recognised.");
            return false;
        }
    }
}
