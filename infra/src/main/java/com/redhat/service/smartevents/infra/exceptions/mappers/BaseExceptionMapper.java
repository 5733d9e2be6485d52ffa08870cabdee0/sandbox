package com.redhat.service.smartevents.infra.exceptions.mappers;

import java.util.Optional;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.api.models.responses.ErrorResponse;
import com.redhat.service.smartevents.infra.api.models.responses.ErrorsResponse;
import com.redhat.service.smartevents.infra.exceptions.BridgeError;
import com.redhat.service.smartevents.infra.exceptions.BridgeErrorService;

import io.quarkus.runtime.Quarkus;

public abstract class BaseExceptionMapper<T extends Exception> implements ExceptionMapper<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseExceptionMapper.class);

    protected BridgeErrorService bridgeErrorService;

    protected Class<? extends RuntimeException> defaultRuntimeException;

    protected BridgeError defaultBridgeError;

    protected BaseExceptionMapper() {
        //CDI proxy
    }

    protected BaseExceptionMapper(BridgeErrorService bridgeErrorService,
            Class<? extends RuntimeException> defaultRuntimeException) {
        this.bridgeErrorService = bridgeErrorService;
        this.defaultRuntimeException = defaultRuntimeException;
    }

    protected void init() {
        Optional<BridgeError> error = bridgeErrorService.getError(defaultRuntimeException);
        if (error.isPresent()) {
            defaultBridgeError = error.get();
        } else {
            LOGGER.error("{} error is not defined in the ErrorsService.", defaultRuntimeException.getSimpleName());
            Quarkus.asyncExit(1);
        }
    }

    protected Response.ResponseBuilder mapError(Throwable e, int statusCode) {
        Optional<BridgeError> error = bridgeErrorService.getError(e.getClass());
        Response.ResponseBuilder builder = Response.status(statusCode);
        if (error.isPresent()) {
            ErrorResponse errorResponse = ErrorResponse.from(error.get());
            errorResponse.setReason(e.getMessage());
            builder.entity(ErrorsResponse.toErrors(errorResponse));
        } else {
            builder.entity(ErrorsResponse.toErrors(unmappedException(e)));
        }
        return builder;
    }

    protected ErrorResponse unmappedException(Throwable e) {
        LOGGER.warn(String.format("Exception '%s' did not link to an '%s'. The raw exception has been wrapped.",
                e.getClass().getSimpleName(),
                defaultRuntimeException.getSimpleName()),
                e);
        ErrorResponse errorResponse = ErrorResponse.from(defaultBridgeError);
        errorResponse.setReason(e.getMessage());
        return errorResponse;
    }

}
