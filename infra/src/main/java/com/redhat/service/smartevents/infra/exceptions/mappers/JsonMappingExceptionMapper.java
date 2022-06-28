package com.redhat.service.smartevents.infra.exceptions.mappers;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.redhat.service.smartevents.infra.api.models.responses.ErrorsResponse;
import com.redhat.service.smartevents.infra.exceptions.BridgeErrorService;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ExternalUserException;

/**
 * Generic mapping of *any* {@link JsonMappingException} into a {@link ErrorsResponse}.
 * The underlying {@link ExternalUserException} should be in the {@link Exception#getCause()}.
 */
public class JsonMappingExceptionMapper extends BaseExceptionMapper<JsonMappingException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonMappingExceptionMapper.class);

    protected JsonMappingExceptionMapper() {
        //CDI proxy
    }

    public JsonMappingExceptionMapper(BridgeErrorService bridgeErrorService) {
        super(bridgeErrorService, ExternalUserException.class);
    }

    @Override
    public Response toResponse(JsonMappingException e) {
        LOGGER.debug("Failure", e);
        Throwable cause = e.getCause();
        ResponseBuilder builder = mapError(cause, getStatusCode(cause));
        return builder.build();
    }

    private int getStatusCode(Throwable cause) {
        if (!(cause instanceof ExternalUserException)) {
            return Response.Status.BAD_REQUEST.getStatusCode();
        }
        ExternalUserException eue = (ExternalUserException) cause;
        return eue.getStatusCode();
    }
}
