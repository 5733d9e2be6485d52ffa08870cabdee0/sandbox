package com.redhat.service.smartevents.processor;

import java.util.stream.Collectors;

import com.networknt.schema.ValidationMessage;
import com.redhat.service.smartevents.infra.models.gateways.Gateway;
import com.redhat.service.smartevents.infra.validations.ValidationResult;

public abstract class AbstractGatewayValidator<T extends Gateway> implements GatewayBean {

    private JsonSchemaService jsonSchemaService;

    public AbstractGatewayValidator() {
    }

    public AbstractGatewayValidator(JsonSchemaService jsonSchemaService) {
        this.jsonSchemaService = jsonSchemaService;
    }

    protected ValidationResult applyAdditionalValidations(T gateway) {
        return ValidationResult.valid();
    }

    public ValidationResult isValid(T gateway) {
        if (gateway.getParameters() == null) {
            return ValidationResult.invalid();
        }

        com.networknt.schema.ValidationResult result = jsonSchemaService.validate(getType(), gateway.getProcessorType(), gateway.getParameters());
        if (result.getValidationMessages().size() > 0) {
            return ValidationResult.invalid(result.getValidationMessages().stream().map(ValidationMessage::getMessage).collect(Collectors.joining(" and ")));
        }

        return applyAdditionalValidations(gateway);
    }
}
