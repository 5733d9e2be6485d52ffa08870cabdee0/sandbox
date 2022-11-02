package com.redhat.service.smartevents.manager.api.user.validators.processors;

import java.util.Collections;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ConstraintValidatorContext;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.ProcessorMissingGatewayException;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.processor.ActionConfigurator;

@ApplicationScoped
public class ProcessorActionConstraintValidator extends BaseGatewayConstraintValidator<ValidProcessorAction, ProcessorRequest> {

    static final String MISSING_ACTION_ERROR = "Processor must have an \"action\" definition";

    protected ProcessorActionConstraintValidator() {
        //CDI proxy
    }

    @Inject
    public ProcessorActionConstraintValidator(ActionConfigurator actionConfigurator) {
        super(actionConfigurator);
    }

    @Override
    public boolean isValid(ProcessorRequest value, ConstraintValidatorContext context) {
        Action action = value.getAction();

        if (action == null) {
            addConstraintViolation(context,
                    MISSING_ACTION_ERROR,
                    Collections.emptyMap(),
                    ProcessorMissingGatewayException::new);
            return false;
        }

        return isValidGateway(value.getAction(), context);
    }
}
