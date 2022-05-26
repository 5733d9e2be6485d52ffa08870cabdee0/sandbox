package com.redhat.service.smartevents.manager.api.user.validators.processors;

import java.util.Collections;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ConstraintValidatorContext;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;
import com.redhat.service.smartevents.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.processor.GatewayConfigurator;

@ApplicationScoped
public class GatewayConstraintValidator extends BaseGatewayConstraintValidator<ValidGateway, ProcessorRequest> {

    static final String MISSING_GATEWAY_ERROR = "Processor must have either \"action\" or \"source\"";
    static final String MULTIPLE_GATEWAY_ERROR = "Processor can't have both \"action\" and \"source\"";
    static final String SOURCE_PROCESSOR_WITH_TRANSFORMATION_ERROR = "Source processors don't support transformations";

    protected GatewayConstraintValidator() {
        //CDI proxy
    }

    @Inject
    public GatewayConstraintValidator(GatewayConfigurator gatewayConfigurator) {
        super(gatewayConfigurator);
    }

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

        // Currently, source processors don't support transformation
        if (value.getType() == ProcessorType.SOURCE && value.getTransformationTemplate() != null) {
            addConstraintViolation(context, SOURCE_PROCESSOR_WITH_TRANSFORMATION_ERROR, Collections.emptyMap());
            return false;
        }

        return action != null
                ? isValidGateway(action, context, gatewayConfigurator::getActionValidator)
                : isValidGateway(source, context, gatewayConfigurator::getSourceValidator);
    }

}
