package com.redhat.developer.manager.api.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.developer.infra.api.APIConstants;
import com.redhat.developer.infra.dto.BridgeDTO;
import com.redhat.developer.infra.dto.BridgeStatus;
import com.redhat.developer.infra.dto.ProcessorDTO;
import com.redhat.developer.manager.BridgesService;
import com.redhat.developer.manager.ProcessorService;
import com.redhat.developer.manager.models.Bridge;
import com.redhat.developer.manager.models.Processor;

import static java.util.stream.Collectors.toList;

@Path(APIConstants.SHARD_API_BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ShardBridgesSyncAPI {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShardBridgesSyncAPI.class);
    private static final List<BridgeStatus> statuses = Arrays.asList(BridgeStatus.REQUESTED, BridgeStatus.PROVISIONING, BridgeStatus.DELETION_REQUESTED);

    @Inject
    BridgesService bridgesService;

    @Inject
    ProcessorService processorService;

    @PUT
    @Path("{id}/processors")
    public Response updateProcessorStatus(ProcessorDTO processorDTO) {
        LOGGER.info("Processing update from shard for Processor with id '{}' for bridge '{}' for customer '{}'", processorDTO.getId(), processorDTO.getBridge().getId(),
                processorDTO.getBridge().getCustomerId());
        processorService.updateProcessorStatus(processorDTO);
        return Response.ok().build();
    }

    @GET
    @Path("/{id}/processors")
    public Response getProcessorsForBridge(@PathParam("id") @NotEmpty String bridgeId) {
        LOGGER.info("Request from Shard for Processors to deploy or delete.");
        return Response.ok(processorService.getProcessorByStatuses(bridgeId, statuses)
                .stream()
                .map(Processor::toDTO)
                .collect(toList()))
                .build();
    }

    @GET
    public Response getBridges() {
        LOGGER.info("[Manager] Shard asks for Bridges to deploy or delete");
        return Response.ok(bridgesService.getBridgesByStatuses(statuses)
                .stream()
                .map(Bridge::toDTO)
                .collect(toList()))
                .build();
    }

    @PUT
    public Response updateBridge(BridgeDTO dto) {
        LOGGER.info("[manager] shard wants to update the Bridge with id '{}' with the status '{}'", dto.getId(), dto.getStatus());
        bridgesService.updateBridge(Bridge.fromDTO(dto));
        return Response.ok().build();
    }
}
