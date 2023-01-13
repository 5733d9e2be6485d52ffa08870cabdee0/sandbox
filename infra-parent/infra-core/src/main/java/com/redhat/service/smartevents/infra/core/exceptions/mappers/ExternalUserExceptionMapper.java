package com.redhat.service.smartevents.infra.core.exceptions.mappers;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.core.exceptions.CompositeBridgeErrorService;
import com.redhat.service.smartevents.infra.core.exceptions.ErrorHrefVersionBuilder;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.ExternalUserException;

public class ExternalUserExceptionMapper extends BaseExceptionMapper<ExternalUserException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalUserExceptionMapper.class);

    protected ExternalUserExceptionMapper() {
        //CDI proxy
    }

    public ExternalUserExceptionMapper(CompositeBridgeErrorService bridgeErrorService, ErrorHrefVersionBuilder hrefBuilder) {
        super(bridgeErrorService, ExternalUserException.class, hrefBuilder);
    }

    @Override
    public Response toResponse(ExternalUserException e) {
        LOGGER.debug("Failure", e);
        ResponseBuilder builder = mapError(e, e.getStatusCode());
        return builder.build();
    }

}
