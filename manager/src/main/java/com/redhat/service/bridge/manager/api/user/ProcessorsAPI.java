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
import com.redhat.service.bridge.infra.models.ListResult;
import com.redhat.service.bridge.infra.models.QueryInfo;
import com.redhat.service.bridge.manager.BridgesService;
import com.redhat.service.bridge.manager.ProcessorService;
import com.redhat.service.bridge.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.bridge.manager.api.models.responses.ProcessorListResponse;
import com.redhat.service.bridge.manager.api.models.responses.ProcessorResponse;
import com.redhat.service.bridge.manager.api.user.validators.actions.ValidActionParams;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.manager.models.Processor;

import io.quarkus.security.Authenticated;

@Tag(name = "Processors API", description = "The API that allow the user to retrieve, create or delete Processors of a Bridge instance.")
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
    BridgesService bridgesService;

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
    @Path("{bridgeIdOrName}/processors/{processorId}")
    public Response getProcessor(@NotEmpty @PathParam("bridgeIdOrName") String bridgeIdOrName, @NotEmpty @PathParam("processorId") String processorId) {
        String customerId = identityResolver.resolve(jwt);
        Bridge bridge = bridgesService.getBridgeByIdOrName(bridgeIdOrName, customerId);
        Processor processor = processorService.getProcessor(processorId, bridge);
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
    @Path("{bridgeIdOrName}/processors")
    public Response listProcessors(@NotEmpty @PathParam("bridgeIdOrName") String bridgeIdOrName, @Valid @BeanParam QueryInfo queryInfo) {
        String customerId = identityResolver.resolve(jwt);
        Bridge bridge = bridgesService.getReadyBridge(bridgeIdOrName, customerId);
        ListResult<Processor> processors = processorService.getProcessors(bridge, queryInfo);
        return Response.ok(ListResponse.fill(processors, new ProcessorListResponse(),
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
    @Path("{bridgeIdOrName}/processors")
    public Response addProcessorToBridge(@PathParam("bridgeIdOrName") @NotEmpty String bridgeIdOrName, @ValidActionParams @Valid ProcessorRequest processorRequest) {
        String customerId = identityResolver.resolve(jwt);
        /* We cannot deploy Processors to a Bridge that is not Available */
        Bridge bridge = bridgesService.getReadyBridge(bridgeIdOrName, customerId);
        Processor processor = processorService.createProcessor(bridge, processorRequest);
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
    @Path("{bridgeIdOrName}/processors/{processorId}")
    public Response deleteProcessor(@PathParam("bridgeIdOrName") String bridgeIdOrName, @PathParam("processorId") String processorId) {
        String customerId = identityResolver.resolve(jwt);
        Bridge bridge = bridgesService.getBridgeByIdOrName(bridgeIdOrName, customerId);
        processorService.deleteProcessor(bridge, processorId);
        return Response.accepted().build();
    }
}
