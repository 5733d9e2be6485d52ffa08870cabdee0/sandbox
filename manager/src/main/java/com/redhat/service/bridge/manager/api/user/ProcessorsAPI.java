package com.redhat.service.bridge.manager.api.user;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
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
import com.redhat.service.bridge.manager.api.user.validators.templates.ValidTransformationTemplate;
import com.redhat.service.bridge.manager.models.Processor;

import io.quarkus.security.Authenticated;

@Tag(name = "Processors", description = "The API that allow the user to retrieve, create or delete Processors of a Bridge instance.")
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
    IdentityResolver identityResolver;

    @Inject
    JsonWebToken jwt;

    @APIResponses(value = {
            @APIResponse(description = "Success.", responseCode = "200",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = SchemaType.OBJECT, implementation = ProcessorResponse.class))),
            @APIResponse(description = "Bad request.", responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(description = "Unauthorized.", responseCode = "401"),
            @APIResponse(description = "Forbidden.", responseCode = "403"),
            @APIResponse(description = "Not found.", responseCode = "404", content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(description = "Internal error.", responseCode = "500", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    @Operation(summary = "Get a Processor of a Bridge instance", description = "Get a Processor of a Bridge instance for the authenticated user.")
    @GET
    @Path("{bridgeId}/processors/{processorId}")
    public Response getProcessor(@NotEmpty @PathParam("bridgeId") String bridgeId, @NotEmpty @PathParam("processorId") String processorId) {
        String customerId = identityResolver.resolve(jwt);
        Processor processor = processorService.getProcessor(processorId, bridgeId, customerId);
        return Response.ok(processorService.toResponse(processor)).build();
    }

    @APIResponses(value = {
            @APIResponse(description = "Success.", responseCode = "200",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = SchemaType.OBJECT, implementation = ProcessorListResponse.class))),
            @APIResponse(description = "Bad request.", responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(description = "Unauthorized.", responseCode = "401"),
            @APIResponse(description = "Forbidden.", responseCode = "403"),
            @APIResponse(description = "Not found.", responseCode = "404", content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(description = "Internal error.", responseCode = "500", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    @Operation(summary = "Get the list of Processors of a Bridge instance", description = "Get the list of Processors of a Bridge instance for the authenticated user.")
    @GET
    @Path("{bridgeId}/processors")
    public Response listProcessors(@NotEmpty @PathParam("bridgeId") String bridgeId, @Valid @BeanParam QueryInfo queryInfo) {
        return Response.ok(ListResponse.fill(processorService.getProcessors(bridgeId, identityResolver.resolve(jwt), queryInfo), new ProcessorListResponse(),
                processorService::toResponse)).build();
    }

    @APIResponses(value = {
            @APIResponse(description = "Accepted.", responseCode = "202",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = SchemaType.OBJECT, implementation = ProcessorResponse.class))),
            @APIResponse(description = "Bad request.", responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(description = "Unauthorized.", responseCode = "401"),
            @APIResponse(description = "Forbidden.", responseCode = "403"),
            @APIResponse(description = "Not found.", responseCode = "404", content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(description = "Internal error.", responseCode = "500", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    @Operation(summary = "Create a Processor of a Bridge instance", description = "Create a Processor of a Bridge instance for the authenticated user.")
    @POST
    @Path("{bridgeId}/processors")
    public Response addProcessorToBridge(@NotEmpty @PathParam("bridgeId") String bridgeId, @ValidActionParams @ValidTransformationTemplate @Valid ProcessorRequest processorRequest) {
        String customerId = identityResolver.resolve(jwt);
        Processor processor = processorService.createProcessor(bridgeId, customerId, processorRequest);
        return Response.accepted(processorService.toResponse(processor)).build();
    }

    @APIResponses(value = {
            @APIResponse(description = "Accepted.", responseCode = "202",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = SchemaType.OBJECT, implementation = ProcessorResponse.class))),
            @APIResponse(description = "Bad request.", responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(description = "Unauthorized.", responseCode = "401"),
            @APIResponse(description = "Forbidden.", responseCode = "403"),
            @APIResponse(description = "Not found.", responseCode = "404", content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(description = "Internal error.", responseCode = "500", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    @Operation(summary = "Update a Processor instance", description = "Update a Processor instance for the authenticated user.")
    @PUT
    @Path("{bridgeId}/processors/{processorId}")
    public Response updateProcessor(@NotEmpty @PathParam("bridgeId") String bridgeId, @NotEmpty @PathParam("processorId") String processorId,
            @ValidActionParams @ValidTransformationTemplate @Valid ProcessorRequest processorRequest) {
        String customerId = identityResolver.resolve(jwt);
        Processor processor = processorService.updateProcessor(processorId, bridgeId, customerId, processorRequest);
        return Response.accepted(processorService.toResponse(processor)).build();
    }

    @APIResponses(value = {
            @APIResponse(description = "Accepted.", responseCode = "202"),
            @APIResponse(description = "Bad request.", responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(description = "Unauthorized.", responseCode = "401"),
            @APIResponse(description = "Forbidden.", responseCode = "403"),
            @APIResponse(description = "Not found.", responseCode = "404", content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(description = "Internal error.", responseCode = "500", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    @Operation(summary = "Delete a Processor of a Bridge instance", description = "Delete a Processor of a Bridge instance for the authenticated user.")
    @DELETE
    @Path("{bridgeId}/processors/{processorId}")
    public Response deleteProcessor(@PathParam("bridgeId") String bridgeId, @PathParam("processorId") String processorId) {
        processorService.deleteProcessor(bridgeId, processorId, identityResolver.resolve(jwt));
        return Response.accepted().build();
    }
}
