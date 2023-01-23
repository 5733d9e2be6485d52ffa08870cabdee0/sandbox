package com.redhat.service.smartevents.infra.core.exceptions;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ErrorHrefVersionBuilderImpl implements ErrorHrefVersionBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorHrefVersionBuilderImpl.class);

    private Instance<ErrorHrefVersionProvider> builders;

    protected ErrorHrefVersionBuilderImpl() {
        //CDI proxy
    }

    @Inject
    public ErrorHrefVersionBuilderImpl(@Any Instance<ErrorHrefVersionProvider> builders) {
        this.builders = builders;
    }

    @Override
    public String buildHref(Throwable e, String id) {
        if (e instanceof HasErrorHref) {
            return ((HasErrorHref) e).getBaseHref() + id;
        }
        Optional<ErrorHrefVersionProvider> builder = builders.stream().filter(x -> x.accepts(e)).findFirst();
        if (builder.isEmpty()) {
            LOGGER.error("Could not retrieve HrefBuilder for exception ", e);
            return null;
        }
        return builder.get().buildHref(id);
    }

    @Override
    public String buildHref(ConstraintViolation<?> cv, String id) {
        Optional<ErrorHrefVersionProvider> builder = builders.stream().filter(x -> x.accepts(cv)).findFirst();
        if (builder.isEmpty()) {
            LOGGER.error("Could not retrieve HrefBuilder for constraint violation " + cv.getRootBeanClass());
            return null;
        }
        return builder.get().buildHref(id);
    }

}
