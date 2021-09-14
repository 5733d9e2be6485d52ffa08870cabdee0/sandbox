package com.redhat.service.bridge.manager.api.user;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.manager.CustomerIdResolver;
import com.redhat.service.bridge.manager.ProcessorService;
import com.redhat.service.bridge.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.bridge.manager.models.Processor;

@Path(APIConstants.USER_API_BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class ProcessorsAPI {

    @Inject
    ProcessorService processorService;

    @Inject
    CustomerIdResolver customerIdResolver;

    @GET
    @Path("{bridgeId}/processors/{processorId}")
    public Response getProcessor(@NotEmpty @PathParam("bridgeId") String bridgeId, @NotEmpty @PathParam("processorId") String processorId) {
        String customerId = customerIdResolver.resolveCustomerId();
        Processor processor = processorService.getProcessor(processorId, bridgeId, customerId);
        return Response.ok(processor.toResponse()).build();
    }

    @POST
    @Path("{bridgeId}/processors")
    public Response addProcessorToBridge(@PathParam("bridgeId") @NotEmpty String bridgeId, @Valid ProcessorRequest processorRequest) {
        String customerId = customerIdResolver.resolveCustomerId();
        Processor processor = processorService.createProcessor(bridgeId, customerId, processorRequest);
        return Response.status(Response.Status.CREATED).entity(processor.toResponse()).build();
    }
}
