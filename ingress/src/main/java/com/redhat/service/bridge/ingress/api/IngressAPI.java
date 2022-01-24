package com.redhat.service.bridge.ingress.api;

import java.net.URI;
import java.security.Principal;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.security.SecuritySchemes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.service.bridge.infra.auth.CustomerIdResolver;
import com.redhat.service.bridge.infra.exceptions.definitions.user.BadRequestException;
import com.redhat.service.bridge.infra.exceptions.definitions.user.ForbiddenRequestException;
import com.redhat.service.bridge.infra.utils.CloudEventUtils;
import com.redhat.service.bridge.ingress.producer.KafkaEventPublisher;

import io.cloudevents.CloudEvent;
import io.cloudevents.SpecVersion;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;

@SecuritySchemes(value = {
        @SecurityScheme(securitySchemeName = "bearer",
                type = SecuritySchemeType.HTTP,
                scheme = "Bearer")
})
@SecurityRequirement(name = "bearer")
@Path("/events")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class IngressAPI {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngressAPI.class);

    @ConfigProperty(name = "event-bridge.bridge.id")
    String bridgeId;

    @ConfigProperty(name = "event-bridge.customer.id")
    String customerId;

    @Inject
    CustomerIdResolver customerIdResolver;

    @Inject
    SecurityIdentity identity;

    @Inject
    KafkaEventPublisher kafkaEventPublisher;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response publishEvent(@NotNull CloudEvent event) {
        failIfNotAuthorized(identity.getPrincipal());
        LOGGER.debug("New event has been uploaded to endpoint /events");
        kafkaEventPublisher.sendEvent(bridgeId, event);
        return Response.ok().build();
    }

    @POST
    @Path("/plain")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response publishEvent(
            @HeaderParam("ce-specversion") @NotNull String cloudEventSpecVersion,
            @HeaderParam("ce-type") @NotNull String cloudEventType,
            @HeaderParam("ce-id") @NotNull String cloudEventId,
            @HeaderParam("ce-source") @NotNull String cloudEventSource,
            @HeaderParam("ce-subject") @NotNull String cloudEventSubject,
            @NotNull JsonNode event) {
        failIfNotAuthorized(identity.getPrincipal());
        LOGGER.debug("New event has been uploaded to endpoint /events/plain");
        validateHeaders(cloudEventSpecVersion, cloudEventSource);
        CloudEvent cloudEvent = CloudEventUtils.build(cloudEventId, SpecVersion.parse(cloudEventSpecVersion),
                URI.create(cloudEventSource), cloudEventSubject, event);
        kafkaEventPublisher.sendEvent(bridgeId, cloudEvent);
        return Response.ok().build();
    }

    private void failIfNotAuthorized(Principal principal) {
        if (!customerIdResolver.resolveCustomerId(principal).equals(customerId)) {
            throw new ForbiddenRequestException("User is not authorized to access this application.");
        }
    }

    private void validateHeaders(String cloudEventSpecVersion, String cloudEventSource) {
        try {
            SpecVersion.parse(cloudEventSpecVersion);
            URI.create(cloudEventSource);
        } catch (Exception e) {
            throw new BadRequestException("Header values not valid: 'ce-specversion' header must be a valid cloud event version and 'ce-source' a valid URI.");
        }
    }
}
