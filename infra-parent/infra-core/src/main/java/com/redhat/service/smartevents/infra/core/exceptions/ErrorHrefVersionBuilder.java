package com.redhat.service.smartevents.infra.core.exceptions;

import javax.validation.ConstraintViolation;

public interface ErrorHrefVersionBuilder {

    String buildHref(Throwable e, String id);

    String buildHref(ConstraintViolation<?> cv, String id);
}
