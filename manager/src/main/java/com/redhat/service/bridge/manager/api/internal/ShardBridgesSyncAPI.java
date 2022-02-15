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

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.security.SecuritySchemes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.auth.CustomerIdResolver;
import com.redhat.service.bridge.infra.exceptions.definitions.user.ForbiddenRequestException;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.manager.BridgesService;
import com.redhat.service.bridge.manager.ProcessorService;

import io.quarkus.security.Authenticated;

import static java.util.stream.Collectors.toList;

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
    private static final List<BridgeStatus> statuses = Arrays.asList(BridgeStatus.ACCEPTED, BridgeStatus.DEPROVISION);

    @ConfigProperty(name = "event-bridge.shard.id")
    String shardId;

    @Inject
    BridgesService bridgesService;

    @Inject
    ProcessorService processorService;

    @Inject
    CustomerIdResolver customerIdResolver;

    @Inject
    JsonWebToken jwt;

    @PUT
    @Path("processors")
    public Response updateProcessorStatus(ProcessorDTO processorDTO) {
        failIfNotAuthorized(jwt);
        LOGGER.info("Processing update from shard for Processor with id '{}' for bridge '{}' for customer '{}'", processorDTO.getId(), processorDTO.getBridgeId(),
                processorDTO.getCustomerId());
        processorService.updateProcessorStatus(processorDTO);
        return Response.ok().build();
    }

    @GET
    @Path("processors")
    public Response getProcessors() {
        failIfNotAuthorized(jwt);
        LOGGER.info("Request from Shard for Processors to deploy or delete.");
        return Response.ok(processorService.getProcessorByStatuses(statuses)
                .stream()
                .map(processorService::toDTO)
                .collect(toList()))
                .build();
    }

    @GET
    public Response getBridges() {
        failIfNotAuthorized(jwt);
        LOGGER.info("Shard asks for Bridges to deploy or delete");
        return Response.ok(bridgesService.getBridgesByStatuses(statuses)
                .stream()
                .map(bridgesService::toDTO)
                .collect(toList()))
                .build();
    }

    @PUT
    public Response updateBridge(BridgeDTO dto) {
        failIfNotAuthorized(jwt);
        LOGGER.info("Shard wants to update the Bridge with id '{}' with the status '{}'", dto.getId(), dto.getStatus());
        bridgesService.updateBridge(dto);
        return Response.ok().build();
    }

    private void failIfNotAuthorized(JsonWebToken jwt) {
        String customerId = customerIdResolver.resolveCustomerId(jwt);
        if (!shardId.equals(customerId)) {
            throw new ForbiddenRequestException(String.format("User '%s' is not authorized to access this api.", customerId));
        }
    }
}
