package com.redhat.developer.manager.api.user;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.redhat.developer.manager.ConnectorsService;
import com.redhat.developer.manager.CustomerIdResolver;
import com.redhat.developer.manager.models.Connector;
import com.redhat.developer.manager.requests.ConnectorRequest;

@Path("/connectors")
public class ConnectorsAPI {

    @Inject
    CustomerIdResolver customerIdResolver;

    @Inject
    ConnectorsService connectorsService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConnectors() {
        List<Connector> connectors = connectorsService.getConnectors(customerIdResolver.resolveCustomerId());
        return Response.ok(connectors.stream().map(Connector::toDTO).collect(Collectors.toList())).build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createConnector(ConnectorRequest connectorRequest) {
        Connector connector = connectorsService.createConnector(customerIdResolver.resolveCustomerId(), connectorRequest);
        return Response.ok(connector.toDTO()).build();
    }

}
