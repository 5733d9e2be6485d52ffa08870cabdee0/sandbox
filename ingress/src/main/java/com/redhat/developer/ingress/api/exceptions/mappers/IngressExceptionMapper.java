package com.redhat.developer.ingress.api.exceptions.mappers;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.developer.ingress.api.exceptions.IngressException;

@Provider
public class IngressExceptionMapper implements ExceptionMapper<IngressException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngressExceptionMapper.class);

    //TODO - Extend this with support for Error codes and useful payload
    @Override
    public Response toResponse(IngressException e) {
        LOGGER.error("Failure", e);
        return Response.status(e.getStatusCode()).entity(e.getMessage()).build();
    }
}
