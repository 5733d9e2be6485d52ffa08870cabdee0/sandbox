package com.redhat.service.bridge.manager.api;

import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.infra.api.exceptions.EventBridgeException;
import com.redhat.service.bridge.manager.ErrorsService;
import com.redhat.service.bridge.manager.api.models.responses.ErrorResponse;
import com.redhat.service.bridge.manager.models.Error;

@Provider
public class EventBridgeExceptionMapper implements ExceptionMapper<EventBridgeException> {

    @Inject
    ErrorsService errorsService;

    private static final Logger LOGGER = LoggerFactory.getLogger(EventBridgeExceptionMapper.class);

    @Override
    public Response toResponse(EventBridgeException e) {
        LOGGER.error("Failure", e);
        Optional<Error> error = errorsService.getError(e);
        ResponseBuilder builder = Response.status(e.getStatusCode());
        if (error.isPresent()) {
            ErrorResponse errorResponse = ErrorResponse.from(error.get());
            errorResponse.setReason(e.getMessage());
            builder.entity(errorResponse);
        } else {
            LOGGER.warn("Information for exception type {} cannot be found", e.getClass());
            builder.entity(e.getMessage());
        }
        return builder.build();
    }
}
