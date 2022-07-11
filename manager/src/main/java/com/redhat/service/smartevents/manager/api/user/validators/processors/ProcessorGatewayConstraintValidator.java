package com.redhat.service.smartevents.manager.api.user.validators.processors;

import java.util.Collections;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ConstraintValidatorContext;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.ProcessorMissingGatewayException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ProcessorMultipleGatewayException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ProcessorTransformationTemplateUnsupportedException;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;
import com.redhat.service.smartevents.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.processor.GatewayConfigurator;

@ApplicationScoped
public class ProcessorGatewayConstraintValidator extends BaseGatewayConstraintValidator<ValidProcessorGateway, ProcessorRequest> {

    static final String MISSING_GATEWAY_ERROR = "Processor must have either \"action\" or \"source\"";
    static final String MULTIPLE_GATEWAY_ERROR = "Processor can't have both \"action\" and \"source\"";
    static final String SOURCE_PROCESSOR_WITH_TRANSFORMATION_ERROR = "Source processors don't support transformations";

    protected ProcessorGatewayConstraintValidator() {
        //CDI proxy
    }

    @Inject
    public ProcessorGatewayConstraintValidator(GatewayConfigurator gatewayConfigurator) {
        super(gatewayConfigurator);
    }

    @Override
    public boolean isValid(ProcessorRequest value, ConstraintValidatorContext context) {
        Action action = value.getAction();
        Source source = value.getSource();

        if (action == null && !value.hasActions() && source == null) {
            addConstraintViolation(context,
                    MISSING_GATEWAY_ERROR,
                    Collections.emptyMap(),
                    ProcessorMissingGatewayException::new);
            return false;
        }
        if (action != null && source != null) {
            addConstraintViolation(context,
                    MULTIPLE_GATEWAY_ERROR,
                    Collections.emptyMap(),
                    ProcessorMultipleGatewayException::new);
            return false;
        }

        // Currently, source processors don't support transformation
        if (value.getType() == ProcessorType.SOURCE && value.getTransformationTemplate() != null) {
            addConstraintViolation(context,
                    SOURCE_PROCESSOR_WITH_TRANSFORMATION_ERROR,
                    Collections.emptyMap(),
                    ProcessorTransformationTemplateUnsupportedException::new);
            return false;
        }

        return value.hasActions() || isValidGateway(value.getGateway(), context);
    }
}
