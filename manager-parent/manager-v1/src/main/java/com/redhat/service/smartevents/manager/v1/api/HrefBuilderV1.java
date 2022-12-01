package com.redhat.service.smartevents.manager.v1.api;

import java.util.Arrays;

import javax.enterprise.context.ApplicationScoped;
import javax.validation.ConstraintViolation;

import com.redhat.service.smartevents.infra.core.exceptions.HrefBuilder;
import com.redhat.service.smartevents.infra.v1.api.V1APIConstants;

@ApplicationScoped
public class HrefBuilderV1 implements HrefBuilder {

    @Override
    public boolean accepts(Throwable e) {
        return Arrays.stream(e.getStackTrace()).anyMatch(x -> x.getClassName().matches("com.redhat.service.smartevents.*.v1.*"));
    }

    @Override
    public boolean accepts(ConstraintViolation<?> cv) {
        return cv.getRootBeanClass().getName().matches("com.redhat.service.smartevents.*.v1.*");
    }

    @Override
    public String buildHref(String id) {
        return V1APIConstants.V1_ERROR_API_BASE_PATH + id;
    }
}
