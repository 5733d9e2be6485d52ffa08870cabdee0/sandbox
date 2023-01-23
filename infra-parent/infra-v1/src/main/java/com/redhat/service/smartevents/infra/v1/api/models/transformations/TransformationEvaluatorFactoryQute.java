package com.redhat.service.smartevents.infra.v1.api.models.transformations;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.infra.core.validations.ValidationResult;
import com.redhat.service.smartevents.infra.v1.api.exceptions.definitions.user.ProcessorTemplateDefinitionException;

import io.quarkus.qute.Engine;
import io.quarkus.qute.TemplateException;

@ApplicationScoped
public class TransformationEvaluatorFactoryQute implements TransformationEvaluatorFactory {

    private static final Engine engine = Engine.builder().addDefaults().build();

    @Override
    public TransformationEvaluator build(String template) {
        return new TransformationEvaluatorQute(engine, template);
    }

    @Override
    public ValidationResult validate(String template) {
        try {
            engine.parse(template);
        } catch (TemplateException e) {
            return ValidationResult.invalid(new ProcessorTemplateDefinitionException(e.getMessage()));
        }
        return ValidationResult.valid();
    }
}
