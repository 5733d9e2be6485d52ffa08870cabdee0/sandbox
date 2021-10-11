package com.redhat.service.bridge.ingress.api;

import java.net.URI;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.service.bridge.infra.utils.CloudEventUtils;
import com.redhat.service.bridge.ingress.IngressService;
import com.redhat.service.bridge.ingress.api.exceptions.BadRequestException;

import io.cloudevents.CloudEvent;
import io.cloudevents.SpecVersion;

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
    public Response publishEvent(@PathParam("name") @NotNull String name, @NotNull CloudEvent event) {
        LOGGER.debug("[ingress] new event has been uploaded to endpoint /ingress/events/{}", name);
        ingressService.processEvent(name, event);
        return Response.ok().build();
    }

    @POST
    @Path("/events/{name}/plain")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response publishEvent(@PathParam("name") @NotNull String name,
            @HeaderParam("ce-specversion") @NotNull String cloudEventSpecVersion,
            @HeaderParam("ce-type") @NotNull String cloudEventType,
            @HeaderParam("ce-id") @NotNull String cloudEventId,
            @HeaderParam("ce-source") @NotNull String cloudEventSource,
            @HeaderParam("ce-subject") @NotNull String cloudEventSubject,
            @NotNull JsonNode event) {
        LOGGER.debug("[ingress] new event has been uploaded to endpoint /ingress/events/{}", name);
        validateHeaders(cloudEventSpecVersion, cloudEventSource);
        CloudEvent cloudEvent = CloudEventUtils.build(cloudEventId, SpecVersion.parse(cloudEventSpecVersion),
                URI.create(cloudEventSource), cloudEventSubject, event);
        ingressService.processEvent(name, cloudEvent);
        return Response.ok().build();
    }

    private void validateHeaders(String cloudEventSpecVersion, String cloudEventSource) {
        try {
            SpecVersion.parse(cloudEventSpecVersion);
            URI.create(cloudEventSource);
        } catch (Exception e) {
            throw new BadRequestException("The specified headers are not valid.");
        }
    }
}
