package com.redhat.service.smartevents.manager.api;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.ext.Provider;

import com.redhat.service.smartevents.infra.exceptions.BridgeErrorService;
import com.redhat.service.smartevents.infra.exceptions.mappers.InternalPlatformExceptionMapper;

import io.quarkus.runtime.Startup;

@Startup
@Provider
@ApplicationScoped
public class ManagerInternalPlatformExceptionMapper extends InternalPlatformExceptionMapper {

    @Inject
    public ManagerInternalPlatformExceptionMapper(BridgeErrorService bridgeErrorService) {
        super(bridgeErrorService);
    }

    @Override
    @PostConstruct
    protected void init() {
        super.init();
    }

}
