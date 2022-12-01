package com.redhat.service.smartevents.infra.core.exceptions;

import javax.validation.ConstraintViolation;

public interface ErrorHrefVersionProvider {
    boolean accepts(Throwable e);

    boolean accepts(ConstraintViolation<?> cv);

    String buildHref(String id);
}
