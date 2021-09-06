package com.redhat.developer.manager.api.user;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.redhat.developer.manager.BridgesService;
import com.redhat.developer.manager.CustomerIdResolver;
import com.redhat.developer.manager.api.models.requests.BridgeRequest;
import com.redhat.developer.manager.api.models.responses.BridgeListResponse;
import com.redhat.developer.manager.api.models.responses.BridgeResponse;
import com.redhat.developer.manager.models.Bridge;

import static com.redhat.developer.manager.api.APIConstants.PAGE;
import static com.redhat.developer.manager.api.APIConstants.PAGE_DEFAULT;
import static com.redhat.developer.manager.api.APIConstants.PAGE_MIN;
import static com.redhat.developer.manager.api.APIConstants.SIZE;
import static com.redhat.developer.manager.api.APIConstants.SIZE_DEFAULT;
import static com.redhat.developer.manager.api.APIConstants.SIZE_MAX;
import static com.redhat.developer.manager.api.APIConstants.SIZE_MIN;

@Path("/api/v1/bridges")
public class BridgesAPI {

    @Inject
    CustomerIdResolver customerIdResolver;

    @Inject
    BridgesService bridgesService;

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBridges(@DefaultValue(PAGE_DEFAULT) @Min(PAGE_MIN) @QueryParam(PAGE) int page,
            @DefaultValue(SIZE_DEFAULT) @Min(SIZE_MIN) @Max(SIZE_MAX) @QueryParam(SIZE) int size) {
        List<BridgeResponse> bridges = bridgesService
                .getBridges(customerIdResolver.resolveCustomerId())
                .stream()
                .map(Bridge::toResponse)
                .collect(Collectors.toList());

        BridgeListResponse response = new BridgeListResponse();
        response.setItems(bridges);
        response.setPage(page);
        response.setSize(bridges.size());
        response.setTotal(-1); // TODO: replace when pagination is implemented

        return Response.ok(response).build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createBridge(BridgeRequest bridgeRequest) {
        Bridge bridge = bridgesService.createBridge(customerIdResolver.resolveCustomerId(), bridgeRequest);
        return Response.ok(bridge.toResponse()).build();
    }

}
