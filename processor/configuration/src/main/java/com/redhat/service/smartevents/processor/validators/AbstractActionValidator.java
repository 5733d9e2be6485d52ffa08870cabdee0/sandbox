package com.redhat.service.smartevents.processor.validators;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.ProcessorGatewayNotRecognisedException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ProcessorGatewayParametersMissingException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ProcessorGatewayParametersNotValidException;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.validations.ValidationResult;
import com.redhat.service.smartevents.processor.ProcessorCatalogService;

public abstract class AbstractActionValidator implements ActionValidator {

    static final String GATEWAY_TYPE_NOT_RECOGNISED_ERROR = "%s of type '%s' is not recognised.";
    static final String GATEWAY_PARAMETERS_MISSING_ERROR = "%s parameters must be supplied";

    private ProcessorCatalogService processorCatalogService;

    protected AbstractActionValidator() {
    }

    protected AbstractActionValidator(ProcessorCatalogService processorCatalogService) {
        this.processorCatalogService = processorCatalogService;
    }

    protected ValidationResult applyAdditionalValidations(Action action) {
        return ValidationResult.valid();
    }

    @Override
    public final ValidationResult isValid(Action action) {
        if (action.getParameters() == null) {
            return ValidationResult.invalid(new ProcessorGatewayParametersMissingException(String.format(GATEWAY_PARAMETERS_MISSING_ERROR,
                    action.getClass().getName())));
        }
        if (processorCatalogService.getActionsCatalog().stream().noneMatch(x -> x.getId().equals(action.getType()))) {
            return ValidationResult.invalid(new ProcessorGatewayNotRecognisedException(String.format(GATEWAY_TYPE_NOT_RECOGNISED_ERROR,
                    action.getClass().getSimpleName(),
                    action.getType())));
        }

        com.networknt.schema.ValidationResult catalogValidationResults = processorCatalogService.validate(action.getType(), action.getProcessorType(), action.getParameters());

        List<ValidationResult.Violation> allViolations = new ArrayList<>();

        // Core validation
        if (!catalogValidationResults.getValidationMessages().isEmpty()) {
            allViolations.addAll(catalogValidationResults.getValidationMessages()
                    .stream()
                    .map(vm -> new ValidationResult.Violation(new ProcessorGatewayParametersNotValidException(vm.getMessage())))
                    .collect(Collectors.toList()));
        }

        // Additional validation
        ValidationResult additionalValidationResults = applyAdditionalValidations(action);
        if (!additionalValidationResults.isValid()) {
            allViolations.addAll(additionalValidationResults.getViolations());
        }

        if (!allViolations.isEmpty()) {
            return ValidationResult.invalid(allViolations);
        }

        return ValidationResult.valid();
    }
}
