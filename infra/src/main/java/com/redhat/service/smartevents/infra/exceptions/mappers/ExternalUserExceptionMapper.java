package com.redhat.service.smartevents.infra.exceptions.mappers;

import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.ExceptionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.api.models.responses.ErrorResponse;
import com.redhat.service.smartevents.infra.api.models.responses.ErrorsResponse;
import com.redhat.service.smartevents.infra.exceptions.BridgeError;
import com.redhat.service.smartevents.infra.exceptions.BridgeErrorService;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ExternalUserException;

import io.quarkus.runtime.Quarkus;

public class ExternalUserExceptionMapper implements ExceptionMapper<ExternalUserException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalUserExceptionMapper.class);

    BridgeError userException;

    @Inject
    BridgeErrorService bridgeErrorService;

    @PostConstruct
    void init() {
        Optional<BridgeError> error = bridgeErrorService.getError(ExternalUserException.class);
        if (error.isPresent()) {
            userException = error.get();
        } else {
            LOGGER.error("ExternalUserException error is not defined in the ErrorsService.");
            Quarkus.asyncExit(1);
        }
    }

    @Override
    public Response toResponse(ExternalUserException e) {
        LOGGER.debug("Failure", e);
        Optional<BridgeError> error = bridgeErrorService.getError(e.getClass());
        ResponseBuilder builder = Response.status(e.getStatusCode());
        if (error.isPresent()) {
            ErrorResponse errorResponse = ErrorResponse.from(error.get());
            errorResponse.setReason(e.getMessage());
            builder.entity(ErrorsResponse.toErrors(errorResponse));
        } else {
            builder.entity(ErrorsResponse.toErrors(unmappedException(e)));
        }
        return builder.build();
    }

    private ErrorResponse unmappedException(Exception e) {
        LOGGER.warn(String.format("Exception %s did not link to an ExternalUserException. The raw exception has been wrapped.", e.getMessage()), e);
        ErrorResponse errorResponse = ErrorResponse.from(userException);
        errorResponse.setReason(e.getMessage());
        return errorResponse;
    }

}
