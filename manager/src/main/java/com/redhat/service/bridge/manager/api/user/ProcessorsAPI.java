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

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.security.SecuritySchemes;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.api.models.responses.ListResponse;
import com.redhat.service.bridge.infra.auth.IdentityResolver;
import com.redhat.service.bridge.infra.models.QueryInfo;
import com.redhat.service.bridge.manager.ProcessorService;
import com.redhat.service.bridge.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.bridge.manager.api.models.responses.ProcessorListResponse;
import com.redhat.service.bridge.manager.api.models.responses.ProcessorResponse;
import com.redhat.service.bridge.manager.api.user.validators.actions.ValidActionParams;
import com.redhat.service.bridge.manager.models.Processor;

import io.quarkus.security.Authenticated;

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
@Tag(name = "Processors")
public class ProcessorsAPI {

    @Inject
    ProcessorService processorService;

    @Inject
    IdentityResolver identityResolver;

    @Inject
    JsonWebToken jwt;

    @GET
    @Path("{bridgeId}/processors/{processorId}")
    @APIResponse(
            responseCode = "200",
            description = "The Processor of given Bridge",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProcessorResponse.class)))
    public Response getProcessor(@NotEmpty @PathParam("bridgeId") String bridgeId, @NotEmpty @PathParam("processorId") String processorId) {
        String customerId = identityResolver.resolve(jwt);
        Processor processor = processorService.getProcessor(processorId, bridgeId, customerId);
        return Response.ok(processorService.toResponse(processor)).build();
    }

    @GET
    @Path("{bridgeId}/processors")
    @APIResponse(
            responseCode = "200",
            description = "The list of Processors of given Bridge",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProcessorListResponse.class)))
    public Response listProcessors(@NotEmpty @PathParam("bridgeId") String bridgeId, @Valid @BeanParam QueryInfo queryInfo) {
        return Response.ok(ListResponse.fill(processorService.getProcessors(bridgeId, identityResolver.resolve(jwt), queryInfo), new ProcessorListResponse(),
                processorService::toResponse)).build();
    }

    @POST
    @Path("{bridgeId}/processors")
    @APIResponse(
            responseCode = "201",
            description = "The Processor added to given Bridge",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProcessorResponse.class)))
    public Response addProcessorToBridge(@PathParam("bridgeId") @NotEmpty String bridgeId, @ValidActionParams @Valid ProcessorRequest processorRequest) {
        String customerId = identityResolver.resolve(jwt);
        Processor processor = processorService.createProcessor(bridgeId, customerId, processorRequest);
        return Response.status(Response.Status.CREATED).entity(processorService.toResponse(processor)).build();
    }

    @DELETE
    @Path("{bridgeId}/processors/{processorId}")
    @APIResponse(
            responseCode = "202",
            description = "Delete the given Processor of given Bridge")
    public Response deleteProcessor(@PathParam("bridgeId") String bridgeId, @PathParam("processorId") String processorId) {
        processorService.deleteProcessor(bridgeId, processorId, identityResolver.resolve(jwt));
        return Response.accepted().build();
    }
}
