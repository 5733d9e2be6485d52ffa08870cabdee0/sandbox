package com.redhat.service.bridge.ingress.api;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.infra.exceptions.definitions.EventBridgeException;

@Provider
public class IngressExceptionMapper implements ExceptionMapper<EventBridgeException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngressExceptionMapper.class);

    //TODO - Extend this with support for Error codes and useful payload
    @Override
    public Response toResponse(EventBridgeException e) {
        LOGGER.error("Failure", e);
        return Response.status(e.getStatusCode()).entity(e.getMessage()).build();
    }
}
