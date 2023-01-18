package com.redhat.service.smartevents.manager.v2.api.internal;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.security.SecuritySchemes;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.core.auth.IdentityResolver;
import com.redhat.service.smartevents.infra.v2.api.V2;
import com.redhat.service.smartevents.infra.v2.api.V2APIConstants;
import com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.user.ForbiddenRequestException;
import com.redhat.service.smartevents.infra.v2.api.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ConditionDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ResourceStatusDTO;
import com.redhat.service.smartevents.infra.v2.api.models.dto.SinkConnectorDTO;
import com.redhat.service.smartevents.manager.core.services.ShardService;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Connector;
import com.redhat.service.smartevents.manager.v2.persistence.models.Processor;
import com.redhat.service.smartevents.manager.v2.services.BridgeService;
import com.redhat.service.smartevents.manager.v2.services.ProcessorService;
import com.redhat.service.smartevents.manager.v2.services.SinkConnectorService;

import io.quarkus.security.Authenticated;

import static java.util.stream.Collectors.toList;

/**
 * This is a private API used by fleet shard operator to sync with the fleet manager so OpenAPI specs are hidden
 */
@SecuritySchemes(value = {
        @SecurityScheme(securitySchemeName = "bearer",
                type = SecuritySchemeType.HTTP,
                scheme = "Bearer")
})
@SecurityRequirement(name = "bearer")
@Path(V2APIConstants.V2_SHARD_API_BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@RegisterRestClient
public class ShardAPI {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShardAPI.class);

    @Inject
    BridgeService bridgesService;

    @Inject
    ProcessorService processorService;

    @Inject
    SinkConnectorService sinkConnectorService;

    @Inject
    ShardService shardService;

    @V2
    @Inject
    IdentityResolver identityResolver;

    @Inject
    JsonWebToken jwt;

    @APIResponses(value = {
            @APIResponse(description = "Success.", responseCode = "200",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = SchemaType.ARRAY, implementation = BridgeDTO.class))),
            @APIResponse(description = "Bad request.", responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(description = "Unauthorized.", responseCode = "401"),
            @APIResponse(description = "Forbidden.", responseCode = "403"),
            @APIResponse(description = "Internal error.", responseCode = "500", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    @Operation(hidden = true, summary = "Get Bridge instances to be processed by a shard.", description = "Get Bridge instances to be processed by a shard.")
    @GET
    public Response getBridges() {
        String shardId = identityResolver.resolve(jwt);
        failIfNotAuthorized(shardId);
        LOGGER.debug("Request from Shard for Bridges to deploy or delete.");
        List<Bridge> bridges = bridgesService.findByShardIdToDeployOrDelete(shardId);
        LOGGER.debug("Found {} bridge(s) to deploy or delete", bridges.size());
        return Response.ok(bridges
                .stream()
                .map(bridgesService::toDTO)
                .collect(toList()))
                .build();
    }

    @APIResponses(value = {
            @APIResponse(description = "Success.", responseCode = "200"),
            @APIResponse(description = "Bad request.", responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(description = "Unauthorized.", responseCode = "401"),
            @APIResponse(description = "Forbidden.", responseCode = "403"),
            @APIResponse(description = "Internal error.", responseCode = "500", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    @Operation(hidden = true, summary = "Update a Bridge status.", description = "Update a Bridge status.")
    @PUT
    public Response updateBridgeStatus(List<ResourceStatusDTO> statusDTOs) {
        String shardId = identityResolver.resolve(jwt);
        failIfNotAuthorized(shardId);

        statusDTOs.forEach(this::updateBridgeStatus);
        return Response.ok().build();
    }

    @APIResponses(value = {
            @APIResponse(description = "Success.", responseCode = "200",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = SchemaType.ARRAY, implementation = ProcessorDTO.class))),
            @APIResponse(description = "Bad request.", responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(description = "Unauthorized.", responseCode = "401"),
            @APIResponse(description = "Forbidden.", responseCode = "403"),
            @APIResponse(description = "Internal error.", responseCode = "500", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    @Operation(hidden = true, summary = "Get Processors to be processed by a shard.", description = "Get Processors to be processed by a shard.")
    @GET
    @Path("processors")
    public Response getProcessors() {
        String shardId = identityResolver.resolve(jwt);
        failIfNotAuthorized(shardId);
        LOGGER.debug("Request from Shard for Processors to deploy or delete.");
        List<Processor> processors = processorService.findByShardIdToDeployOrDelete(shardId);
        LOGGER.debug("Found {} processor(s) to deploy or delete", processors.size());
        return Response.ok(processors
                .stream()
                .map(processorService::toDTO)
                .collect(toList()))
                .build();
    }

    @APIResponses(value = {
            @APIResponse(description = "Success.", responseCode = "200"),
            @APIResponse(description = "Bad request.", responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(description = "Unauthorized.", responseCode = "401"),
            @APIResponse(description = "Forbidden.", responseCode = "403"),
            @APIResponse(description = "Internal error.", responseCode = "500", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    @Operation(hidden = true, summary = "Update Processors' status.", description = "Update Processors' status.")
    @PUT
    @Path("processors")
    public Response updateProcessorsStatus(List<ResourceStatusDTO> statusDTOs) {
        String shardId = identityResolver.resolve(jwt);
        failIfNotAuthorized(shardId);

        statusDTOs.forEach(this::updateProcessorStatus);
        return Response.ok().build();
    }

    @APIResponses(value = {
            @APIResponse(description = "Success.", responseCode = "200",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = SchemaType.ARRAY, implementation = SinkConnectorDTO.class))),
            @APIResponse(description = "Bad request.", responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(description = "Unauthorized.", responseCode = "401"),
            @APIResponse(description = "Forbidden.", responseCode = "403"),
            @APIResponse(description = "Internal error.", responseCode = "500", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    @Operation(hidden = true, summary = "Get Sink Connectors to be processed by a shard.", description = "Get Sink Connectors to be processed by a shard.")
    @GET
    @Path("sinks")
    public Response getSinkConnectors() {
        String shardId = identityResolver.resolve(jwt);
        failIfNotAuthorized(shardId);
        LOGGER.debug("Request from Shard for Sink Connectors to deploy or delete.");
        List<Connector> connectors = sinkConnectorService.findByShardIdToDeployOrDelete(shardId);
        LOGGER.debug("Found {} Sink Connector(s) to deploy or delete", connectors.size());
        return Response.ok(connectors
                .stream()
                .map(sinkConnectorService::toDTO)
                .collect(toList()))
                .build();
    }

    @APIResponses(value = {
            @APIResponse(description = "Success.", responseCode = "200"),
            @APIResponse(description = "Bad request.", responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(description = "Unauthorized.", responseCode = "401"),
            @APIResponse(description = "Forbidden.", responseCode = "403"),
            @APIResponse(description = "Internal error.", responseCode = "500", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    @Operation(hidden = true, summary = "Update Sink Connector's status.", description = "Update Sink Connector's status.")
    @PUT
    @Path("sinks")
    public Response updateConnectorsStatus(List<ResourceStatusDTO> statusDTOs) {
        throw new RuntimeException("Not implemented yet exception https://issues.redhat.com/browse/MGDOBR-1380");
    }

    private void updateProcessorStatus(ResourceStatusDTO statusDTO) {
        try {
            LOGGER.debug("Processing update from Shard for Processor with id '{}' and generation '{}' with conditions '{}'",
                    statusDTO.getId(),
                    statusDTO.getGeneration(),
                    logConditions(statusDTO.getConditions()));
            processorService.updateProcessorStatus(statusDTO);
        } catch (Exception e) {
            LOGGER.warn("Failed to process update from Shard for Processor with id '{}' and generation '{}' with conditions '{}'",
                    statusDTO.getId(),
                    statusDTO.getGeneration(),
                    logConditions(statusDTO.getConditions()),
                    e);
        }

    }

    private void updateBridgeStatus(ResourceStatusDTO statusDTO) {
        try {
            LOGGER.debug("Processing update from Shard for Bridge with id '{}' and generation '{}' with conditions '{}'",
                    statusDTO.getId(),
                    statusDTO.getGeneration(),
                    logConditions(statusDTO.getConditions()));
            bridgesService.updateBridgeStatus(statusDTO);
        } catch (Exception e) {
            LOGGER.warn("Failed to process update from Shard for Bridge with id '{}' and generation '{}' with conditions '{}'",
                    statusDTO.getId(),
                    statusDTO.getGeneration(),
                    logConditions(statusDTO.getConditions()),
                    e);
        }
    }

    private String logConditions(List<ConditionDTO> conditions) {
        return conditions.stream().map(c -> new StringBuilder(c.getType())
                .append(" = ")
                .append(c.getStatus())).collect(Collectors.joining(", "));
    }

    private void failIfNotAuthorized(String shardId) {
        if (!shardService.isAuthorizedShard(shardId)) {
            throw new ForbiddenRequestException(String.format("User '%s' is not authorized to access this api.", shardId));
        }
    }
}
