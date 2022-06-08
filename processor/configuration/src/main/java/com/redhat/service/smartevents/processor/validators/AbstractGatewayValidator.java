package com.redhat.service.smartevents.processor.validators;

import java.util.stream.Collectors;

import com.networknt.schema.ValidationMessage;
import com.redhat.service.smartevents.infra.models.gateways.Gateway;
import com.redhat.service.smartevents.infra.validations.ValidationResult;
import com.redhat.service.smartevents.processor.ProcessorCatalogService;

public abstract class AbstractGatewayValidator implements GatewayValidator {

    private ProcessorCatalogService processorCatalogService;

    protected AbstractGatewayValidator() {
    }

    protected AbstractGatewayValidator(ProcessorCatalogService processorCatalogService) {
        this.processorCatalogService = processorCatalogService;
    }

    protected ValidationResult applyAdditionalValidations(Gateway gateway) {
        return ValidationResult.valid();
    }

    @Override
    public final ValidationResult isValid(Gateway gateway) {
        if (gateway.getParameters() == null) {
            return ValidationResult.invalid();
        }

        com.networknt.schema.ValidationResult result = processorCatalogService.validate(gateway.getType(), gateway.getProcessorType(), gateway.getParameters());
        if (!result.getValidationMessages().isEmpty()) {
            return ValidationResult.invalid(result.getValidationMessages().stream().map(ValidationMessage::getMessage).collect(Collectors.joining(" and ")));
        }

        return applyAdditionalValidations(gateway);
    }
}
