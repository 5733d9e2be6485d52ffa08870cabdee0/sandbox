package com.redhat.service.smartevents.manager.api;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.ext.Provider;

import com.redhat.service.smartevents.infra.exceptions.BridgeErrorService;
import com.redhat.service.smartevents.infra.exceptions.mappers.ConstraintViolationExceptionMapper;

@Provider
@ApplicationScoped
public class ManagerConstraintViolationExceptionMapper extends ConstraintViolationExceptionMapper {

    @Inject
    public ManagerConstraintViolationExceptionMapper(BridgeErrorService bridgeErrorService) {
        super(bridgeErrorService);
    }

    @Override
    @PostConstruct
    protected void init() {
        super.init();
    }

}
