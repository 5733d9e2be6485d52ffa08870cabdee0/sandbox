package com.redhat.service.smartevents.infra.v1.api.exceptions.definitions.user;

import javax.ws.rs.core.Response;

public class ProcessorTransformationTemplateUnsupportedException extends BaseExternalUserException {

    private static final long serialVersionUID = 1L;

    public ProcessorTransformationTemplateUnsupportedException(String message) {
        super(message);
    }

    public ProcessorTransformationTemplateUnsupportedException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public int getStatusCode() {
        return Response.Status.BAD_REQUEST.getStatusCode();
    }
}
