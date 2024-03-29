package com.redhat.service.smartevents.infra.core.exceptions.mappers;

import java.util.Optional;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.core.exceptions.BridgeError;
import com.redhat.service.smartevents.infra.core.exceptions.CompositeBridgeErrorService;
import com.redhat.service.smartevents.infra.core.exceptions.ErrorHrefVersionBuilder;
import com.redhat.service.smartevents.infra.core.models.responses.ErrorResponse;
import com.redhat.service.smartevents.infra.core.models.responses.ErrorsResponse;

import io.quarkus.runtime.Quarkus;

public abstract class BaseExceptionMapper<T extends Exception> implements ExceptionMapper<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseExceptionMapper.class);

    protected CompositeBridgeErrorService bridgeErrorService;

    protected Class<? extends RuntimeException> defaultRuntimeException;

    protected BridgeError defaultBridgeError;

    protected ErrorHrefVersionBuilder hrefBuilder;

    protected BaseExceptionMapper() {
        //CDI proxy
    }

    protected BaseExceptionMapper(CompositeBridgeErrorService bridgeErrorService,
            Class<? extends RuntimeException> defaultRuntimeException,
            ErrorHrefVersionBuilder hrefBuilder) {
        this.bridgeErrorService = bridgeErrorService;
        this.defaultRuntimeException = defaultRuntimeException;
        this.hrefBuilder = hrefBuilder;
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
            ErrorResponse errorResponse = toErrorResponse(error.get());
            errorResponse.setReason(e.getMessage());
            errorResponse.setHref(hrefBuilder.buildHref(e, errorResponse.getId()));
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
        ErrorResponse errorResponse = toErrorResponse(defaultBridgeError);
        errorResponse.setReason(e.getMessage());
        errorResponse.setHref(hrefBuilder.buildHref(e, errorResponse.getId()));
        return errorResponse;
    }

    protected ErrorResponse toErrorResponse(BridgeError be) {
        return ErrorResponse.from(be);
    }

}
