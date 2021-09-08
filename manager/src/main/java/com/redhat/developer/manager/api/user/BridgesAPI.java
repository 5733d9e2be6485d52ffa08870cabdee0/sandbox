package com.redhat.developer.manager.api.user;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;
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

import com.redhat.developer.manager.BridgesService;
import com.redhat.developer.manager.CustomerIdResolver;
import com.redhat.developer.manager.ProcessorService;
import com.redhat.developer.manager.api.models.requests.BridgeRequest;
import com.redhat.developer.manager.api.models.requests.ProcessorRequest;
import com.redhat.developer.manager.api.models.responses.BridgeListResponse;
import com.redhat.developer.manager.api.models.responses.BridgeResponse;
import com.redhat.developer.manager.models.Bridge;
import com.redhat.developer.manager.models.ListResult;
import com.redhat.developer.manager.models.Processor;

import static com.redhat.developer.manager.api.APIConstants.PAGE;
import static com.redhat.developer.manager.api.APIConstants.PAGE_DEFAULT;
import static com.redhat.developer.manager.api.APIConstants.PAGE_MIN;
import static com.redhat.developer.manager.api.APIConstants.PAGE_SIZE;
import static com.redhat.developer.manager.api.APIConstants.SIZE_DEFAULT;
import static com.redhat.developer.manager.api.APIConstants.SIZE_MAX;
import static com.redhat.developer.manager.api.APIConstants.SIZE_MIN;

@Path("/api/v1/bridges")
public class BridgesAPI {

    @Inject
    CustomerIdResolver customerIdResolver;

    @Inject
    BridgesService bridgesService;

    @Inject
    ProcessorService processorService;

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
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBridge(@PathParam("id") @NotEmpty String id) {
        Bridge bridge = bridgesService.getBridge(id, customerIdResolver.resolveCustomerId());
        return Response.ok(bridge.toResponse()).build();
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteBridge(@PathParam("id") String id) {
        bridgesService.deleteBridge(id, customerIdResolver.resolveCustomerId());
        return Response.accepted().build();
    }

    @Path("/{id}/processors")
    @POST
    public Response addProcessorToBridge(@PathParam("id") @NotEmpty String id, @Valid ProcessorRequest processorRequest) {
        String customerId = customerIdResolver.resolveCustomerId();
        Processor processor = processorService.createProcessor(customerId, id, processorRequest);
        return Response.status(Response.Status.CREATED).entity(processor.toResponse()).build();
    }
}
