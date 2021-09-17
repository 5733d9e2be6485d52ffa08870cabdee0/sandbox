package com.redhat.service.bridge.manager.api.user;

import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
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
import com.redhat.service.bridge.manager.CustomerIdResolver;
import com.redhat.service.bridge.manager.ProcessorService;
import com.redhat.service.bridge.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.bridge.manager.api.models.responses.ProcessorListResponse;
import com.redhat.service.bridge.manager.api.models.responses.ProcessorResponse;
import com.redhat.service.bridge.manager.models.ListResult;
import com.redhat.service.bridge.manager.models.Processor;

import static com.redhat.service.bridge.infra.api.APIConstants.PAGE;
import static com.redhat.service.bridge.infra.api.APIConstants.PAGE_DEFAULT;
import static com.redhat.service.bridge.infra.api.APIConstants.PAGE_MIN;
import static com.redhat.service.bridge.infra.api.APIConstants.PAGE_SIZE;
import static com.redhat.service.bridge.infra.api.APIConstants.SIZE_DEFAULT;
import static com.redhat.service.bridge.infra.api.APIConstants.SIZE_MAX;
import static com.redhat.service.bridge.infra.api.APIConstants.SIZE_MIN;
import static java.util.stream.Collectors.toList;

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

    @GET
    @Path("{bridgeId}/processors")
    public Response listProcessors(@NotEmpty @PathParam("bridgeId") String bridgeId, @DefaultValue(PAGE_DEFAULT) @Min(PAGE_MIN) @QueryParam(PAGE) int page,
            @DefaultValue(SIZE_DEFAULT) @Min(SIZE_MIN) @Max(SIZE_MAX) @QueryParam(PAGE_SIZE) int pageSize) {

        String customerId = customerIdResolver.resolveCustomerId();
        ListResult<Processor> processors = processorService.getProcessors(bridgeId, customerId, page, pageSize);
        List<ProcessorResponse> px = processors.getItems().stream().map((p) -> p.toResponse()).collect(toList());

        ProcessorListResponse listResponse = new ProcessorListResponse();
        listResponse.setItems(px);
        listResponse.setPage(processors.getPage());
        listResponse.setSize(processors.getSize());
        listResponse.setTotal(processors.getTotal());
        return Response.ok(listResponse).build();
    }

    @POST
    @Path("{bridgeId}/processors")
    public Response addProcessorToBridge(@PathParam("bridgeId") @NotEmpty String bridgeId, @Valid ProcessorRequest processorRequest) {
        String customerId = customerIdResolver.resolveCustomerId();
        Processor processor = processorService.createProcessor(bridgeId, customerId, processorRequest);
        return Response.status(Response.Status.CREATED).entity(processor.toResponse()).build();
    }
}
