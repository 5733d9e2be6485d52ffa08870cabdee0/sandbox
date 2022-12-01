package com.redhat.service.smartevents.infra.core.exceptions.mappers;

import javax.enterprise.inject.Instance;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.core.exceptions.BridgeErrorService;
import com.redhat.service.smartevents.infra.core.exceptions.ErrorHrefVersionProvider;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.InternalPlatformException;
import com.redhat.service.smartevents.infra.core.models.responses.ErrorResponse;
import com.redhat.service.smartevents.infra.core.models.responses.ErrorsResponse;

public class InternalPlatformExceptionMapper extends BaseExceptionMapper<InternalPlatformException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InternalPlatformExceptionMapper.class);
    private static final String MESSAGE_TEMPLATE = "There was an internal exception that is not fixable from the user. Please " +
            "open a bug with all the information you have, including the name of the exception %s and the message %s";

    protected InternalPlatformExceptionMapper() {
        //CDI proxy
    }

    public InternalPlatformExceptionMapper(BridgeErrorService bridgeErrorService, Instance<ErrorHrefVersionProvider> builders) {
        super(bridgeErrorService, InternalPlatformException.class, builders);
    }

    @Override
    public Response toResponse(InternalPlatformException e) {
        LOGGER.warn("Internal Platform exception", e);

        // InternalPlatformException is always returned.
        Response.ResponseBuilder builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
        ErrorResponse errorResponse = toErrorResponse(e);
        errorResponse.setReason(String.format(MESSAGE_TEMPLATE, e.getClass().getName(), e.getMessage()));
        builder.entity(ErrorsResponse.toErrors(errorResponse));
        return builder.build();
    }

    protected ErrorResponse toErrorResponse(InternalPlatformException e) {
        ErrorResponse errorResponse = toErrorResponse(defaultBridgeError);
        errorResponse.setReason(String.format(MESSAGE_TEMPLATE, e.getClass().getName(), e.getMessage()));
        errorResponse.setHref(buildHrefFromApiVersion(e, errorResponse.getId()));
        return errorResponse;
    }
}
