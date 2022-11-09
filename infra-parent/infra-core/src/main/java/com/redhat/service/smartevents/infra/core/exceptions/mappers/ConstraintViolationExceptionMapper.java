package com.redhat.service.smartevents.infra.core.exceptions.mappers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.hibernate.validator.engine.HibernateConstraintViolation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.core.exceptions.BridgeError;
import com.redhat.service.smartevents.infra.core.exceptions.BridgeErrorService;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.UnclassifiedConstraintViolationException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.ExternalUserException;
import com.redhat.service.smartevents.infra.core.models.ListResult;
import com.redhat.service.smartevents.infra.core.models.responses.ErrorResponse;
import com.redhat.service.smartevents.infra.core.models.responses.ErrorsResponse;

public class ConstraintViolationExceptionMapper extends BaseExceptionMapper<ConstraintViolationException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConstraintViolationExceptionMapper.class);

    private final ErrorResponseConverter converter = new ErrorResponseConverter();

    protected ConstraintViolationExceptionMapper() {
        //CDI proxy
    }

    public ConstraintViolationExceptionMapper(BridgeErrorService bridgeErrorService) {
        super(bridgeErrorService, UnclassifiedConstraintViolationException.class);
    }

    @Override
    public Response toResponse(ConstraintViolationException e) {
        LOGGER.debug(String.format("ConstraintViolationException: %s", e.getMessage()), e);

        ResponseBuilder builder = Response.status(Response.Status.BAD_REQUEST.getStatusCode());
        List<ConstraintViolation<?>> violations = new ArrayList<>(e.getConstraintViolations());

        ErrorsResponse response = new ErrorsResponse();
        ErrorsResponse.fill(new ListResult<>(violations), response, converter);
        return builder.entity(response).build();
    }

    private class ErrorResponseConverter implements Function<ConstraintViolation<?>, ErrorResponse> {

        @Override
        public ErrorResponse apply(ConstraintViolation<?> cv) {
            if (!(cv instanceof HibernateConstraintViolation)) {
                return unmappedConstraintViolation(cv);
            }

            ExternalUserException eue = ((HibernateConstraintViolation<?>) cv).getDynamicPayload(ExternalUserException.class);
            if (Objects.isNull(eue)) {
                return unmappedConstraintViolation(cv);
            }

            Optional<BridgeError> error = bridgeErrorService.getError(eue.getClass());
            if (error.isEmpty()) {
                return unmappedConstraintViolation(cv);
            }

            ErrorResponse errorResponse = toErrorResponse(error.get());
            errorResponse.setReason(eue.getMessage());
            return errorResponse;
        }

        private ErrorResponse unmappedConstraintViolation(ConstraintViolation<?> cv) {
            LOGGER.warn(String.format("ConstraintViolation %s did not link to an ExternalUserException. The raw violation has been wrapped.", cv), cv);
            ErrorResponse errorResponse = toErrorResponse(cv);
            errorResponse.setReason(cv.toString());
            return errorResponse;
        }

    }

    protected ErrorResponse toErrorResponse(ConstraintViolation<?> cv) {
        ErrorResponse errorResponse = toErrorResponse(defaultBridgeError);
        errorResponse.setReason(cv.toString());
        return errorResponse;
    }

}
