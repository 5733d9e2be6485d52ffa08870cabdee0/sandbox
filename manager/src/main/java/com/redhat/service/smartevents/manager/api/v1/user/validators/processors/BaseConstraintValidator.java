package com.redhat.service.smartevents.manager.api.v1.user.validators.processors;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.MessageInterpolator;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.hibernate.validator.internal.engine.MessageInterpolatorContext;
import org.hibernate.validator.messageinterpolation.ExpressionLanguageFeatureLevel;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.ExternalUserException;

abstract class BaseConstraintValidator<A extends Annotation, T> implements ConstraintValidator<A, T> {

    protected void addConstraintViolation(ConstraintValidatorContext context,
            String message,
            Map<String, Object> messageParams,
            Function<String, ExternalUserException> userExceptionSupplier) {
        context.disableDefaultConstraintViolation();
        HibernateConstraintValidatorContext hibernateContext = context.unwrap(HibernateConstraintValidatorContext.class);
        hibernateContext.withDynamicPayload(userExceptionSupplier.apply(interpolateMessage(message, messageParams)));

        if (!messageParams.isEmpty()) {
            messageParams.forEach(hibernateContext::addMessageParameter);
        }
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }

    protected String interpolateMessage(String message, Map<String, Object> messageParams) {
        // Use a minimal Message Interpolator for our purposes.
        // We only use a template and parameters; so it is (reasonably) safe to pass nulls, but put in a try {..} catch() block to be sure.
        // Tests, both programmatic, and real did not reveal any issue but let's not tempt an RTE when live!
        MessageInterpolator interpolator = new ParameterMessageInterpolator();
        MessageInterpolatorContext ic = new MessageInterpolatorContext(null, null, null, null, messageParams, Collections.emptyMap(), ExpressionLanguageFeatureLevel.DEFAULT, false);
        try {
            return interpolator.interpolate(message, ic);
        } catch (Exception e) {
            return message;
        }
    }
}
