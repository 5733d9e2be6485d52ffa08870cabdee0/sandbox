package com.redhat.service.smartevents.manager.api.user.validators.templates;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Target({ ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TransformationTemplateValidatorContainer.class)
public @interface ValidTransformationTemplate {

    String message() default "The supplied transformation template is not valid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
