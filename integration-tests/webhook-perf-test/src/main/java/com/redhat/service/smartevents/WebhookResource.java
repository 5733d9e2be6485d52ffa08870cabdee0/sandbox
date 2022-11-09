package com.redhat.service.smartevents;

import java.util.List;

import javax.inject.Inject;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.redhat.service.smartevents.performance.webhook.exceptions.BridgeNotFoundException;
import com.redhat.service.smartevents.performance.webhook.exceptions.EventNotFoundException;
import com.redhat.service.smartevents.performance.webhook.models.Event;
import com.redhat.service.smartevents.performance.webhook.models.WebhookRequest;
import com.redhat.service.smartevents.performance.webhook.services.WebhookService;
import lombok.extern.slf4j.Slf4j;

import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

@Path("/events")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class WebhookResource {

    @Inject
    WebhookService webhookService;

    @GET
    public Response getAll() {
        log.info("getting all events");
        return Response.ok(webhookService.findAll()).build();
    }

    @GET
    @Path("/{bridgeId}/count")
    public Response countEventsReceived(@NotEmpty @PathParam("bridgeId") String bridgeId) {
        Long count = webhookService.countEventsReceived(bridgeId);
        log.info("{} events received for {}", count, bridgeId);
        return Response.ok(count).build();
    }

    @GET
    @Path("/{bridgeId}")
    public Response getBridgeEvents(@NotEmpty @PathParam("bridgeId") String bridgeId) {
        List<Event> events;
        try {
            log.info("getting events for bridge {}", bridgeId);
            events = webhookService.getEventsByBridgeId(bridgeId);
        } catch (BridgeNotFoundException e) {
            return Response.status(NOT_FOUND).build();
        }
        return Response.ok(events).build();
    }

    @GET
    @Path("/{bridgeId}/{id}")
    public Response getEventById(@NotEmpty @PathParam("bridgeId") String bridgeId, @NotEmpty @PathParam("id") Long id) {
        Event event;
        try {
            log.info("getting id event {} from bridge {}", id, bridgeId);
            event = webhookService.getEvent(id);
            log.info("returning event {}", event);
        } catch (EventNotFoundException e) {
            return Response.status(NOT_FOUND).build();
        }
        return Response.ok(event).build();
    }

    @POST
    public Response consumeEvent(WebhookRequest request) {
        log.info("request received {}", request);
        if (request.getBridgeId() != null && !request.getBridgeId().isEmpty()) {
            Event event = webhookService.create(request.toEntity());
            log.info("new event created {}", event);
            return Response.status(CREATED).entity(event).build();
        } else {
            log.warn("skipping event creation");
            return Response.accepted().build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@NotEmpty @PathParam("id") Long id) {
        try {
            log.info("deleting event id {}", id);
            return Response.ok(webhookService.delete(id)).build();
        } catch (EventNotFoundException e) {
            return Response.status(NOT_FOUND).build();
        }
    }

    @DELETE
    public Response deleteAll() {
        log.info("deleting all events");
        return Response.ok(webhookService.deleteAll()).build();
    }
}