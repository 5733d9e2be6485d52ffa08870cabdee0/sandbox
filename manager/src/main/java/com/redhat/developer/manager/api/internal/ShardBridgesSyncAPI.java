package com.redhat.developer.manager.api.internal;

import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.developer.infra.dto.BridgeDTO;
import com.redhat.developer.manager.BridgesService;
import com.redhat.developer.manager.models.Bridge;

@Path("/api/v1/shard/bridges")
public class ShardBridgesSyncAPI {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShardBridgesSyncAPI.class);

    @Inject
    BridgesService bridgesService;

    @GET
    @Path("/toDeploy")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBridgesToDeploy() {
        LOGGER.info("[Manager] Shard asks for Bridges to deploy");
        return Response.ok(bridgesService.getBridgesToDeploy().stream().map(Bridge::toDTO).collect(Collectors.toList())).build();
    }

    @POST
    @Path("/toDeploy")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response notifyDeployment(BridgeDTO dto) {
        LOGGER.info("[manager] shard wants to update the Bridge with id '{}' with the status '{}'", dto.getId(), dto.getStatus());
        bridgesService.updateBridge(Bridge.fromDTO(dto));
        return Response.ok().build();
    }
}
