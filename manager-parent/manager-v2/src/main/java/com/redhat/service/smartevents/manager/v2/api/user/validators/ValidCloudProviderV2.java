package com.redhat.service.smartevents.manager.v2.api.user.validators;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CloudProviderConstraintValidatorV2.class)
public @interface ValidCloudProviderV2 {

    String message() default "The supplied Cloud Provider details are not valid.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
