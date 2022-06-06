package com.redhat.service.smartevents.processor;

import java.util.stream.Collectors;

import com.networknt.schema.ValidationMessage;
import com.redhat.service.smartevents.infra.models.gateways.Gateway;
import com.redhat.service.smartevents.infra.validations.ValidationResult;

public abstract class AbstractGatewayValidator<T extends Gateway> implements GatewayBean {

    private ProcessorCatalogService processorCatalogService;

    protected AbstractGatewayValidator() {
    }

    protected AbstractGatewayValidator(ProcessorCatalogService processorCatalogService) {
        this.processorCatalogService = processorCatalogService;
    }

    protected ValidationResult applyAdditionalValidations(T gateway) {
        return ValidationResult.valid();
    }

    public final ValidationResult isValid(T gateway) {
        if (gateway.getParameters() == null) {
            return ValidationResult.invalid();
        }

        com.networknt.schema.ValidationResult result = processorCatalogService.validate(getType(), gateway.getProcessorType(), gateway.getParameters());
        if (!result.getValidationMessages().isEmpty()) {
            return ValidationResult.invalid(result.getValidationMessages().stream().map(ValidationMessage::getMessage).collect(Collectors.joining(" and ")));
        }

        return applyAdditionalValidations(gateway);
    }
}
