package com.redhat.service.smartevents.manager.api.user.validators.processors;

import java.lang.annotation.Annotation;
import java.util.Collections;

import javax.validation.ConstraintValidatorContext;

import org.hibernate.validator.internal.engine.messageinterpolation.util.InterpolationHelper;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.ProcessorGatewayParametersMissingException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ProcessorGatewayUnclassifiedException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ProcessorMissingGatewayException;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.validations.ValidationResult;
import com.redhat.service.smartevents.processor.ActionConfigurator;
import com.redhat.service.smartevents.processor.validators.ActionValidator;

abstract class BaseGatewayConstraintValidator<A extends Annotation, T> extends BaseConstraintValidator<A, T> {

    static final String GATEWAY_TYPE_MISSING_ERROR = "{gatewayClass} type must be specified";
    static final String GATEWAY_PARAMETERS_MISSING_ERROR = "{gatewayClass} parameters must be supplied";

    static final String GATEWAY_CLASS_PARAM = "gatewayClass";
    static final String TYPE_PARAM = "type";

    ActionConfigurator actionConfigurator;

    BaseGatewayConstraintValidator() {
        //CDI proxy
    }

    BaseGatewayConstraintValidator(ActionConfigurator actionConfigurator) {
        this.actionConfigurator = actionConfigurator;
    }

    protected boolean isValidGateway(Action action, ConstraintValidatorContext context) {
        if (action.getType() == null) {
            addConstraintViolation(context, GATEWAY_TYPE_MISSING_ERROR,
                    Collections.singletonMap(GATEWAY_CLASS_PARAM, action.getClass().getSimpleName()),
                    ProcessorMissingGatewayException::new);
            return false;
        }

        if (action.getParameters() == null) {
            addConstraintViolation(context, GATEWAY_PARAMETERS_MISSING_ERROR,
                    Collections.singletonMap(GATEWAY_CLASS_PARAM, action.getClass().getSimpleName()),
                    ProcessorGatewayParametersMissingException::new);
            return false;
        }

        ActionValidator validator = actionConfigurator.getValidator(action.getType());
        ValidationResult v = validator.isValid(action);

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
