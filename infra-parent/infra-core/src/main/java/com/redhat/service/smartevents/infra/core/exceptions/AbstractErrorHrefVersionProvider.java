package com.redhat.service.smartevents.infra.core.exceptions;

import java.util.Arrays;

import javax.validation.ConstraintViolation;

public abstract class AbstractErrorHrefVersionProvider implements ErrorHrefVersionProvider {

    @Override
    public boolean accepts(Throwable e) {
        return Arrays.stream(e.getStackTrace()).anyMatch(x -> x.getClassName().matches(getPackageRegexMatch()));
    }

    @Override
    public boolean accepts(ConstraintViolation<?> cv) {
        return cv.getRootBeanClass().getName().matches(getPackageRegexMatch());

    }

    @Override
    public String buildHref(String id) {
        return getBaseHref() + id;
    }

    protected abstract String getPackageRegexMatch();

    protected abstract String getBaseHref();
}
