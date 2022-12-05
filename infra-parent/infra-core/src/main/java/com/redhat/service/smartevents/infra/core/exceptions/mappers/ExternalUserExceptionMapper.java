package com.redhat.service.smartevents.infra.core.exceptions.mappers;

import javax.enterprise.inject.Instance;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.core.exceptions.BridgeErrorService;
import com.redhat.service.smartevents.infra.core.exceptions.ErrorHrefVersionProvider;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.ExternalUserException;

public class ExternalUserExceptionMapper extends BaseExceptionMapper<ExternalUserException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalUserExceptionMapper.class);

    protected ExternalUserExceptionMapper() {
        //CDI proxy
    }

    public ExternalUserExceptionMapper(BridgeErrorService bridgeErrorService, Instance<ErrorHrefVersionProvider> builders) {
        super(bridgeErrorService, ExternalUserException.class, builders);
    }

    @Override
    public Response toResponse(ExternalUserException e) {
        LOGGER.debug("Failure", e);
        ResponseBuilder builder = mapError(e, e.getStatusCode());
        return builder.build();
    }

}
