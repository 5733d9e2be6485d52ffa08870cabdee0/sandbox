package com.redhat.service.bridge.manager.api.user;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.security.SecuritySchemes;

import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.models.QueryInfo;
import com.redhat.service.bridge.manager.CustomerIdResolver;
import com.redhat.service.bridge.manager.ProcessorService;
import com.redhat.service.bridge.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.bridge.manager.api.models.responses.ListResponse;
import com.redhat.service.bridge.manager.api.models.responses.ProcessorListResponse;
import com.redhat.service.bridge.manager.api.user.validators.actions.ValidActionParams;
import com.redhat.service.bridge.manager.models.Processor;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;

@SecuritySchemes(value = {
        @SecurityScheme(securitySchemeName = "bearer",
                type = SecuritySchemeType.HTTP,
                scheme = "Bearer")
})
@SecurityRequirement(name = "bearer")
@Path(APIConstants.USER_API_BASE_PATH)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class ProcessorsAPI {

    @Inject
    ProcessorService processorService;

    @Inject
    CustomerIdResolver customerIdResolver;

    @Inject
    SecurityIdentity identity;

    @GET
    @Path("{bridgeId}/processors/{processorId}")
    public Response getProcessor(@NotEmpty @PathParam("bridgeId") String bridgeId, @NotEmpty @PathParam("processorId") String processorId) {
        String customerId = customerIdResolver.resolveCustomerId(identity.getPrincipal());
        Processor processor = processorService.getProcessor(processorId, bridgeId, customerId);
        return Response.ok(processorService.toResponse(processor)).build();
    }

    @GET
    @Path("{bridgeId}/processors")
    public Response listProcessors(@NotEmpty @PathParam("bridgeId") String bridgeId, @Valid @BeanParam QueryInfo queryInfo) {
        return Response.ok(ListResponse.fill(processorService.getProcessors(bridgeId, customerIdResolver.resolveCustomerId(identity.getPrincipal()), queryInfo), new ProcessorListResponse(),
                processorService::toResponse)).build();
    }

    @POST
    @Path("{bridgeId}/processors")
    public Response addProcessorToBridge(@PathParam("bridgeId") @NotEmpty String bridgeId, @ValidActionParams @Valid ProcessorRequest processorRequest) {
        String customerId = customerIdResolver.resolveCustomerId(identity.getPrincipal());
        Processor processor = processorService.createProcessor(bridgeId, customerId, processorRequest);
        return Response.status(Response.Status.CREATED).entity(processorService.toResponse(processor)).build();
    }

    @DELETE
    @Path("{bridgeId}/processors/{processorId}")
    public Response deleteProcessor(@PathParam("bridgeId") String bridgeId, @PathParam("processorId") String processorId) {
        processorService.deleteProcessor(bridgeId, processorId, customerIdResolver.resolveCustomerId(identity.getPrincipal()));
        return Response.accepted().build();
    }
}
