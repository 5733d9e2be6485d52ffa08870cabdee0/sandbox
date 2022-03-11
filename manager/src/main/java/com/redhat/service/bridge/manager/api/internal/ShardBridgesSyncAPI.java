package com.redhat.service.bridge.manager.api.internal;

import java.util.Arrays;
import java.util.List;

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
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.auth.IdentityResolver;
import com.redhat.service.bridge.infra.exceptions.definitions.user.ForbiddenRequestException;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.manager.BridgesService;
import com.redhat.service.bridge.manager.ProcessorService;
import com.redhat.service.bridge.manager.ShardService;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.manager.models.Processor;

import io.quarkus.security.Authenticated;

import static java.util.stream.Collectors.toList;

@Tag(name = "Shard API", description = "The API that allow a shard to retrieve and update resources.")
@SecuritySchemes(value = {
        @SecurityScheme(securitySchemeName = "bearer",
                type = SecuritySchemeType.HTTP,
                scheme = "Bearer")
})
@SecurityRequirement(name = "bearer")
@Path(APIConstants.SHARD_API_BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class ShardBridgesSyncAPI {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShardBridgesSyncAPI.class);

    public static final List<ManagedResourceStatus> SHARD_REQUESTED_STATUS = Arrays.asList(ManagedResourceStatus.ACCEPTED, ManagedResourceStatus.DEPROVISION);

    @Inject
    BridgesService bridgesService;

    @Inject
    ProcessorService processorService;

    @Inject
    ShardService shardService;

    @Inject
    IdentityResolver identityResolver;

    @Inject
    JsonWebToken jwt;

    @APIResponses(value = {
            @APIResponse(description = "Success.", responseCode = "200"),
            @APIResponse(description = "Bad request.", responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(description = "Unauthorized.", responseCode = "401"),
            @APIResponse(description = "Forbidden.", responseCode = "403")
    })
    @Operation(summary = "Update a processor.", description = "Update a processor.")
    @PUT
    @Path("processors")
    public Response updateProcessorStatus(ProcessorDTO processorDTO) {
        String shardId = identityResolver.resolve(jwt);
        failIfNotAuthorized(shardId);
        LOGGER.info("Processing update from shard for Processor with id '{}' for bridge '{}' for customer '{}' with status '{}'",
                processorDTO.getId(),
                processorDTO.getBridgeId(),
                processorDTO.getCustomerId(),
                processorDTO.getStatus());
        processorService.updateProcessorStatus(processorDTO);
        return Response.ok().build();
    }

    @APIResponses(value = {
            @APIResponse(description = "Success.", responseCode = "200",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = SchemaType.ARRAY, implementation = ProcessorDTO.class))),
            @APIResponse(description = "Bad request.", responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(description = "Unauthorized.", responseCode = "401"),
            @APIResponse(description = "Forbidden.", responseCode = "403")
    })
    @Operation(summary = "Get processors to be processed by a shard.", description = "Get processors to be processed by a shard.")
    @GET
    @Path("processors")
    public Response getProcessors() {
        String shardId = identityResolver.resolve(jwt);
        failIfNotAuthorized(shardId);
        LOGGER.info("Request from Shard for Processors to deploy or delete.");
        List<Processor> processorToDeployOrDelete = processorService.findByShardIdWithReadyDependencies(shardId);
        LOGGER.info("Found {} processor(s) to deploy or delete", processorToDeployOrDelete.size());
        return Response.ok(processorToDeployOrDelete
                .stream()
                .map(processorService::toDTO)
                .collect(toList()))
                .build();
    }

    @APIResponses(value = {
            @APIResponse(description = "Success.", responseCode = "200",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = SchemaType.ARRAY, implementation = BridgeDTO.class))),
            @APIResponse(description = "Bad request.", responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(description = "Unauthorized.", responseCode = "401"),
            @APIResponse(description = "Forbidden.", responseCode = "403")
    })
    @Operation(summary = "Get ingresses to be processed by a shard.", description = "Get ingresses to be processed by a shard.")
    @GET
    public Response getBridges() {
        String shardId = identityResolver.resolve(jwt);
        failIfNotAuthorized(shardId);
        LOGGER.info("Shard asks for Bridges to deploy or delete");
        List<Bridge> bridgesToDeployOrDelete = bridgesService.getBridgesByStatusesAndShardId(SHARD_REQUESTED_STATUS, shardId);
        LOGGER.info("Found {} bridge(s) to deploy or delete", bridgesToDeployOrDelete.size());
        return Response.ok(bridgesToDeployOrDelete
                .stream()
                .map(bridgesService::toDTO)
                .collect(toList()))
                .build();
    }

    @APIResponses(value = {
            @APIResponse(description = "Success.", responseCode = "200"),
            @APIResponse(description = "Bad request.", responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(description = "Unauthorized.", responseCode = "401"),
            @APIResponse(description = "Forbidden.", responseCode = "403")
    })
    @Operation(summary = "Update an ingress.", description = "Update an ingress.")
    @PUT
    public Response updateBridge(BridgeDTO dto) {
        String subject = identityResolver.resolve(jwt);
        failIfNotAuthorized(subject);
        LOGGER.info("Shard wants to update the Bridge with id '{}' with the status '{}'", dto.getId(), dto.getStatus());
        bridgesService.updateBridge(dto);
        return Response.ok().build();
    }

    private void failIfNotAuthorized(String shardId) {
        if (!shardService.isAuthorizedShard(shardId)) {
            throw new ForbiddenRequestException(String.format("User '%s' is not authorized to access this api.", shardId));
        }
    }
}
