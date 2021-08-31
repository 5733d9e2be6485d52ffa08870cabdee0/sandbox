package com.redhat.developer.ingress.api;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.developer.ingress.IngressService;

//TODO: when we move to k8s, revisit the endpoint name
@Path("/ingress")
public class IngressAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(IngressAPI.class);

    @Inject
    IngressService ingressService;

    @POST
    @Path("/events/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response publishEvent(@PathParam("name") @NotNull String name, @NotNull JsonNode event) {
        LOGGER.info(String.format("[ingress] new event has been uploaded to endpoint /events/" + name));
        ingressService.processEvent(name, event);
        return Response.ok().build();
    }
}
