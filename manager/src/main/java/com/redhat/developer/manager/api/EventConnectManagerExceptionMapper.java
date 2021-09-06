package com.redhat.developer.manager.api;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.developer.manager.exceptions.EventConnectManagerException;

@Provider
public class EventConnectManagerExceptionMapper implements ExceptionMapper<EventConnectManagerException> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventConnectManagerExceptionMapper.class);

    //TODO - Extend this with support for Error codes and useful payload
    @Override
    public Response toResponse(EventConnectManagerException e) {
        LOGGER.error("Failure", e);
        return Response.status(e.getStatusCode()).entity(e.getMessage()).build();
    }
}
