package com.redhat.service.smartevents.manager.api.user.validators.processors;

import java.lang.annotation.Annotation;
import java.util.Map;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

abstract class BaseConstraintValidator<A extends Annotation, T> implements ConstraintValidator<A, T> {

    protected void addConstraintViolation(ConstraintValidatorContext context, String message, Map<String, Object> messageParams) {
        context.disableDefaultConstraintViolation();
        if (!messageParams.isEmpty()) {
            HibernateConstraintValidatorContext hibernateContext = context.unwrap(HibernateConstraintValidatorContext.class);
            messageParams.forEach(hibernateContext::addMessageParameter);
        }
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
