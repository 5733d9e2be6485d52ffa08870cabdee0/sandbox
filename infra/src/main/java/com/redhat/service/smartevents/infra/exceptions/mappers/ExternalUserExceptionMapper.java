package com.redhat.service.smartevents.infra.exceptions.mappers;

import java.util.Optional;

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
import com.redhat.service.smartevents.infra.exceptions.definitions.platform.InternalPlatformException;
import com.redhat.service.smartevents.infra.exceptions.definitions.platform.UnclassifiedException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ExternalUserException;

public class ExternalUserExceptionMapper implements ExceptionMapper<ExternalUserException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalUserExceptionMapper.class);

    @Inject
    BridgeErrorService bridgeErrorService;

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
        Optional<BridgeError> error = bridgeErrorService.getError(UnclassifiedException.class);
        if (error.isEmpty()) {
            throw new InternalPlatformException("Lookup of UnclassifiedException failed.");
        }
        ErrorResponse errorResponse = ErrorResponse.from(error.get());
        errorResponse.setReason(e.getMessage());
        return errorResponse;
    }

}
