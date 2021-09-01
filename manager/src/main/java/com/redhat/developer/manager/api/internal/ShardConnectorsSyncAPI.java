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

import com.redhat.developer.infra.dto.ConnectorDTO;
import com.redhat.developer.manager.ConnectorsService;
import com.redhat.developer.manager.models.Connector;

@Path("/shard/connectors")
public class ShardConnectorsSyncAPI {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShardConnectorsSyncAPI.class);

    @Inject
    ConnectorsService connectorsService;

    @GET
    @Path("/toDeploy")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConnectorsToDeploy() {
        LOGGER.info("[Manager] Shard asks for connectors to deploy");
        return Response.ok(connectorsService.getConnectorsToDeploy().stream().map(Connector::toDTO).collect(Collectors.toList())).build();
    }

    @POST
    @Path("/toDeploy")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response notifyDeployment(ConnectorDTO dto) {
        LOGGER.info("[manager] shard wants to update the connector with id '{}' with the status '{}'", dto.getId(), dto.getStatus());
        connectorsService.updateConnector(Connector.fromDTO(dto));
        return Response.ok().build();
    }
}
