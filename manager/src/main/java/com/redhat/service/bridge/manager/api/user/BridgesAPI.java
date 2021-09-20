package com.redhat.service.bridge.manager.api.user;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.manager.BridgesService;
import com.redhat.service.bridge.manager.CustomerIdResolver;
import com.redhat.service.bridge.manager.api.models.requests.BridgeRequest;
import com.redhat.service.bridge.manager.api.models.responses.BridgeListResponse;
import com.redhat.service.bridge.manager.api.models.responses.BridgeResponse;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.manager.models.ListResult;

import static com.redhat.service.bridge.infra.api.APIConstants.PAGE;
import static com.redhat.service.bridge.infra.api.APIConstants.PAGE_DEFAULT;
import static com.redhat.service.bridge.infra.api.APIConstants.PAGE_MIN;
import static com.redhat.service.bridge.infra.api.APIConstants.PAGE_SIZE;
import static com.redhat.service.bridge.infra.api.APIConstants.SIZE_DEFAULT;
import static com.redhat.service.bridge.infra.api.APIConstants.SIZE_MAX;
import static com.redhat.service.bridge.infra.api.APIConstants.SIZE_MIN;

@Path(APIConstants.USER_API_BASE_PATH)
public class BridgesAPI {

    @Inject
    CustomerIdResolver customerIdResolver;

    @Inject
    BridgesService bridgesService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBridges(@DefaultValue(PAGE_DEFAULT) @Min(PAGE_MIN) @QueryParam(PAGE) int page,
            @DefaultValue(SIZE_DEFAULT) @Min(SIZE_MIN) @Max(SIZE_MAX) @QueryParam(PAGE_SIZE) int pageSize) {
        ListResult<Bridge> bridges = bridgesService
                .getBridges(customerIdResolver.resolveCustomerId(), page, pageSize);

        List<BridgeResponse> bridgeResponses = bridges.getItems()
                .stream()
                .map(Bridge::toResponse)
                .collect(Collectors.toList());

        BridgeListResponse response = new BridgeListResponse();
        response.setItems(bridgeResponses);
        response.setPage(bridges.getPage());
        response.setSize(bridges.getSize());
        response.setTotal(bridges.getTotal());

        return Response.ok(response).build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createBridge(BridgeRequest bridgeRequest) {
        Bridge bridge = bridgesService.createBridge(customerIdResolver.resolveCustomerId(), bridgeRequest);
        return Response.status(Response.Status.CREATED).entity(bridge.toResponse()).build();
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBridge(@PathParam("id") @NotEmpty String id) {
        Bridge bridge = bridgesService.getBridge(id, customerIdResolver.resolveCustomerId());
        return Response.ok(bridge.toResponse()).build();
    }

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteBridge(@PathParam("id") String id) {
        bridgesService.deleteBridge(id, customerIdResolver.resolveCustomerId());
        return Response.accepted().build();
    }
}
