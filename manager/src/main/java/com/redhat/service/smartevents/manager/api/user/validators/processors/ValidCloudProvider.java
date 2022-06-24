package com.redhat.service.smartevents.manager.api.user.validators.processors;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CloudProviderConstraintValidator.class)
public @interface ValidCloudProvider {

}
