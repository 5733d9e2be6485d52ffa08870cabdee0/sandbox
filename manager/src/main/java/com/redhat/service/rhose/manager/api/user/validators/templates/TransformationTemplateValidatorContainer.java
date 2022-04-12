package com.redhat.service.rhose.manager.api.user.validators.templates;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

import com.redhat.service.rhose.infra.transformations.TransformationEvaluatorFactory;
import com.redhat.service.rhose.infra.validations.ValidationResult;
import com.redhat.service.rhose.manager.api.models.requests.ProcessorRequest;

@ApplicationScoped
public class TransformationTemplateValidatorContainer implements ConstraintValidator<ValidTransformationTemplate, ProcessorRequest> {

    static final String TRANSFORMATION_TEMPLATE_MALFORMED_ERROR = "Transformation template malformed: {error}";
    static final String ERROR_PARAM = "error";

    @Inject
    TransformationEvaluatorFactory transformationEvaluatorFactory;

    @Override
    public boolean isValid(ProcessorRequest value, ConstraintValidatorContext context) {
        String transformationTemplate = value.getTransformationTemplate();
        if (transformationTemplate == null) {
            return true;
        }

        ValidationResult validationResult = transformationEvaluatorFactory.validate(transformationTemplate);
        if (validationResult.isValid()) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        HibernateConstraintValidatorContext hibernateContext = context.unwrap(HibernateConstraintValidatorContext.class);
        hibernateContext.addMessageParameter(ERROR_PARAM, validationResult.getMessage());
        hibernateContext.buildConstraintViolationWithTemplate(TRANSFORMATION_TEMPLATE_MALFORMED_ERROR).addConstraintViolation();
        return false;
    }
}
