package com.redhat.service.smartevents.processor.validators;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.ProcessorGatewayNotRecognisedException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ProcessorGatewayParametersMissingException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ProcessorGatewayParametersNotValidException;
import com.redhat.service.smartevents.infra.models.gateways.Gateway;
import com.redhat.service.smartevents.infra.validations.ValidationResult;
import com.redhat.service.smartevents.processor.ProcessorCatalogService;

public abstract class AbstractGatewayValidator implements GatewayValidator {

    static final String GATEWAY_TYPE_NOT_RECOGNISED_ERROR = "%s of type '%s' is not recognised.";
    static final String GATEWAY_PARAMETERS_MISSING_ERROR = "%s parameters must be supplied";

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
            return ValidationResult.invalid(new ProcessorGatewayParametersMissingException(String.format(GATEWAY_PARAMETERS_MISSING_ERROR,
                    gateway.getClass().getName())));
        }
        if (processorCatalogService.getActionsCatalog().stream().noneMatch(x -> x.getId().equals(gateway.getType())) &&
                processorCatalogService.getSourcesCatalog().stream().noneMatch(x -> x.getId().equals(gateway.getType()))) {
            return ValidationResult.invalid(new ProcessorGatewayNotRecognisedException(String.format(GATEWAY_TYPE_NOT_RECOGNISED_ERROR,
                    gateway.getClass().getSimpleName(),
                    gateway.getType())));
        }

        com.networknt.schema.ValidationResult catalogValidationResults = processorCatalogService.validate(gateway.getType(), gateway.getProcessorType(), gateway.getParameters());

        List<ValidationResult.Violation> allViolations = new ArrayList<>();

        // Core validation
        if (!catalogValidationResults.getValidationMessages().isEmpty()) {
            allViolations.addAll(catalogValidationResults.getValidationMessages()
                    .stream()
                    .map(vm -> new ValidationResult.Violation(new ProcessorGatewayParametersNotValidException(vm.getMessage())))
                    .collect(Collectors.toList()));
        }

        // Additional validation
        ValidationResult additionalValidationResults = applyAdditionalValidations(gateway);
        if (!additionalValidationResults.isValid()) {
            allViolations.addAll(additionalValidationResults.getViolations());
        }

        if (!allViolations.isEmpty()) {
            return ValidationResult.invalid(allViolations);
        }

        return ValidationResult.valid();
    }
}
