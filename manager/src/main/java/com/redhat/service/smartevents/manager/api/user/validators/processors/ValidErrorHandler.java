package com.redhat.service.smartevents.manager.api.user.validators.processors;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ErrorHandlerConstraintValidator.class)
public @interface ValidErrorHandler {

    String message() default "The supplied Action parameters are not valid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
