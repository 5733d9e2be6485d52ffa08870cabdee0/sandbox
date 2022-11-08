package com.redhat.service.smartevents.manager.v1.api.user.validators.processors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.ProcessorTemplateDefinitionException;
import com.redhat.service.smartevents.infra.core.validations.ValidationResult;
import com.redhat.service.smartevents.infra.v1.api.models.transformations.TransformationEvaluatorFactory;

@ApplicationScoped
public class TransformationTemplateConstraintValidator implements ConstraintValidator<ValidTransformationTemplate, String> {

    static final String TRANSFORMATION_TEMPLATE_MALFORMED_ERROR = "Transformation template malformed: {error}";
    static final String ERROR_PARAM = "error";

    @Inject
    TransformationEvaluatorFactory transformationEvaluatorFactory;

    @Override
    public boolean isValid(String transformationTemplate, ConstraintValidatorContext context) {
        if (transformationTemplate == null) {
            return true;
        }

        ValidationResult validationResult = transformationEvaluatorFactory.validate(transformationTemplate);
        if (validationResult.isValid()) {
            return true;
        }

        ValidationResult.Violation violation = validationResult.getViolations().get(0);

        context.disableDefaultConstraintViolation();
        HibernateConstraintValidatorContext hibernateContext = context.unwrap(HibernateConstraintValidatorContext.class);
        hibernateContext
                .withDynamicPayload(new ProcessorTemplateDefinitionException(violation.getException().getMessage()))
                .addMessageParameter(ERROR_PARAM, violation.getException().getMessage())
                .buildConstraintViolationWithTemplate(TRANSFORMATION_TEMPLATE_MALFORMED_ERROR)
                .addConstraintViolation();
        return false;
    }
}
