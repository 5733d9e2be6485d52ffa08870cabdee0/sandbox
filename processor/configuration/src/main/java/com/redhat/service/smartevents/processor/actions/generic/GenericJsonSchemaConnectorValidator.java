package com.redhat.service.smartevents.processor.actions.generic;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.validations.ValidationResult;
import com.redhat.service.smartevents.processor.GatewayValidator;
import com.redhat.service.smartevents.processor.validation.JsonSchemaService;

public abstract class GenericJsonSchemaConnectorValidator implements GatewayValidator<Action> {

    @Inject
    JsonSchemaService jsonSchemaService;

    @Override
    public ValidationResult isValid(Action action) {

        JsonSchema schema = jsonSchemaService.findJsonSchemaForConnectorName(action.getType());

        schema.initializeValidators();
        ObjectNode parameters = action.getRawParameters();

        // hack as the topic will be set later so validation should pass
        parameters.set("kafka_topic", new TextNode("dummytopic"));

        Set<ValidationMessage> errors = schema.validate(parameters);

        if (!errors.isEmpty()) {
            String errorsString = errors.stream().map(Objects::toString).collect(Collectors.joining("|"));
            return new ValidationResult(false, errorsString);
        }

        parameters.remove("kafka_topic"); // make sure this hack doesn't leak

        return new ValidationResult(true);
    }
}
