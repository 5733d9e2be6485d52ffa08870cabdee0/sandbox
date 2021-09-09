package com.redhat.developer.manager.api.internal;

import java.util.Arrays;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.developer.infra.api.APIConstants;
import com.redhat.developer.infra.dto.BridgeDTO;
import com.redhat.developer.infra.dto.BridgeStatus;
import com.redhat.developer.manager.BridgesService;
import com.redhat.developer.manager.models.Bridge;

@Path(APIConstants.SHARD_API_BASE_PATH)
public class ShardBridgesSyncAPI {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShardBridgesSyncAPI.class);
    private static final List<BridgeStatus> statuses = Arrays.asList(BridgeStatus.REQUESTED, BridgeStatus.DELETION_REQUESTED);

    @Inject
    BridgesService bridgesService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBridges() {
        LOGGER.info("[Manager] Shard asks for Bridges to deploy or delete");
        return Response.ok(bridgesService.getBridgesByStatuses(statuses)
                .stream()
                .map(Bridge::toDTO)
                .collect(Collectors.toList()))
                .build();
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateBridge(BridgeDTO dto) {
        LOGGER.info("[manager] shard wants to update the Bridge with id '{}' with the status '{}'", dto.getId(), dto.getStatus());
        bridgesService.updateBridge(Bridge.fromDTO(dto));
        return Response.ok().build();
    }
}
