package com.redhat.service.smartevents.manager.v2.api;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.ext.Provider;

import com.redhat.service.smartevents.infra.core.exceptions.BridgeErrorService;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.InternalPlatformException;
import com.redhat.service.smartevents.infra.core.exceptions.mappers.InternalPlatformExceptionMapper;
import com.redhat.service.smartevents.infra.core.models.responses.ErrorResponse;
import com.redhat.service.smartevents.infra.v2.api.V2APIConstants;

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

    @Override
    protected ErrorResponse toErrorResponse(InternalPlatformException e) {
        ErrorResponse errorResponse = super.toErrorResponse(e);
        errorResponse.setHref(V2APIConstants.V2_ERROR_API_BASE_PATH + errorResponse.getId());
        return errorResponse;
    }

}
