package com.redhat.service.bridge.ingress.api;

import java.net.URI;

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
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.security.SecuritySchemes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.service.bridge.infra.auth.IdentityResolver;
import com.redhat.service.bridge.infra.exceptions.definitions.user.BadRequestException;
import com.redhat.service.bridge.infra.exceptions.definitions.user.ForbiddenRequestException;
import com.redhat.service.bridge.infra.utils.CloudEventUtils;
import com.redhat.service.bridge.ingress.producer.KafkaEventPublisher;

import io.cloudevents.CloudEvent;
import io.cloudevents.SpecVersion;
import io.quarkus.security.Authenticated;

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

    @ConfigProperty(name = "event-bridge.customer.id")
    String customerId;

    @ConfigProperty(name = "event-bridge.webhook.technical-account-id")
    String webhookTechnicalAccountId;

    @Inject
    JsonWebToken jwt;

    @Inject
    IdentityResolver identityResolver;

    @Inject
    KafkaEventPublisher kafkaEventPublisher;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response publishEvent(@NotNull CloudEvent event) {
        failIfNotAuthorized(jwt);
        LOGGER.debug("New event has been uploaded to endpoint /events");
        kafkaEventPublisher.sendEvent(event);
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
            @HeaderParam("ce-subject") String cloudEventSubject,
            @NotNull JsonNode event) {
        failIfNotAuthorized(jwt);
        LOGGER.debug("New event has been uploaded to endpoint /events/plain");
        validateHeaders(cloudEventSpecVersion, cloudEventSource);
        CloudEvent cloudEvent = CloudEventUtils.build(cloudEventId, SpecVersion.parse(cloudEventSpecVersion),
                URI.create(cloudEventSource), cloudEventSubject, event);
        kafkaEventPublisher.sendEvent(cloudEvent);
        return Response.ok().build();
    }

    private void failIfNotAuthorized(JsonWebToken jwt) {
        String subject = identityResolver.resolve(jwt);
        LOGGER.info(subject);
        if (!customerId.equals(subject) && !webhookTechnicalAccountId.equals(subject)) {
            throw new ForbiddenRequestException(String.format("User '%s' is not authorized to access this api.", subject));
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
