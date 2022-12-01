package com.redhat.service.smartevents.manager.core.api;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.ws.rs.ext.Provider;

import com.redhat.service.smartevents.infra.core.exceptions.BridgeErrorService;
import com.redhat.service.smartevents.infra.core.exceptions.HrefBuilder;
import com.redhat.service.smartevents.infra.core.exceptions.mappers.ConstraintViolationExceptionMapper;
import com.redhat.service.smartevents.infra.core.models.responses.ErrorResponse;

@Provider
@ApplicationScoped
public class ManagerConstraintViolationExceptionMapper extends ConstraintViolationExceptionMapper {

    @Inject
    public ManagerConstraintViolationExceptionMapper(BridgeErrorService bridgeErrorService, Instance<HrefBuilder> builders) {
        super(bridgeErrorService, builders);
    }

    @Override
    @PostConstruct
    protected void init() {
        super.init();
    }

    @Override
    protected ErrorResponse toErrorResponse(ConstraintViolation<?> cv) {
        return super.toErrorResponse(cv);
    }
}
