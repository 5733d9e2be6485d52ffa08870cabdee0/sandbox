package com.redhat.service.smartevents.manager.api.user;

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

import com.redhat.service.smartevents.infra.api.APIConstants;
import com.redhat.service.smartevents.infra.api.models.responses.ListResponse;
import com.redhat.service.smartevents.infra.auth.IdentityResolver;
import com.redhat.service.smartevents.infra.models.QueryInfo;
import com.redhat.service.smartevents.manager.BridgesService;
import com.redhat.service.smartevents.manager.api.models.requests.BridgeRequest;
import com.redhat.service.smartevents.manager.api.models.responses.BridgeListResponse;
import com.redhat.service.smartevents.manager.api.models.responses.BridgeResponse;
import com.redhat.service.smartevents.manager.models.Bridge;

import io.quarkus.security.Authenticated;

@Tag(name = "Bridges", description = "The API that allow the user to retrieve, create or delete Bridge instances.")
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
public class BridgesAPI {

    @Inject
    IdentityResolver identityResolver;

    @Inject
    BridgesService bridgesService;

    @Inject
    JsonWebToken jwt;

    @APIResponses(value = {
            @APIResponse(description = "Success.", responseCode = "200",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = SchemaType.OBJECT, ref="BridgeListResponse", implementation = BridgeListResponse.class))),
            @APIResponse(description = "Bad request.", responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(description = "Unauthorized.", responseCode = "401"),
            @APIResponse(description = "Forbidden.", responseCode = "403"),
            @APIResponse(description = "Not found.", responseCode = "404", content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(description = "Internal error.", responseCode = "500", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    @Operation(summary = "Get the list of Bridge instances", description = "Get the list of Bridge instances for the authenticated user.")
    @GET
    public Response getBridges(@Valid @BeanParam QueryInfo queryInfo) {
        return Response.ok(ListResponse.fill(bridgesService
                .getBridges(identityResolver.resolve(jwt), queryInfo), new BridgeListResponse(), bridgesService::toResponse)).build();
    }

    @APIResponses(value = {
            @APIResponse(description = "Accepted.", responseCode = "202",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = SchemaType.OBJECT,ref="BridgeResponse", implementation = BridgeResponse.class))),
            @APIResponse(description = "Bad request.", responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(description = "Unauthorized.", responseCode = "401"),
            @APIResponse(description = "Forbidden.", responseCode = "403"),
            @APIResponse(description = "Internal error.", responseCode = "500", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    @Operation(summary = "Create a Bridge instance", description = "Create a Bridge instance for the authenticated user.")
    @POST
    public Response createBridge(@Valid BridgeRequest bridgeRequest) {
        Bridge bridge = bridgesService.createBridge(identityResolver.resolve(jwt), bridgeRequest);
        return Response.accepted(bridgesService.toResponse(bridge)).build();
    }

    @APIResponses(value = {
            @APIResponse(description = "Success.", responseCode = "200",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = SchemaType.OBJECT, ref="BridgeResponse", implementation = BridgeResponse.class))),
            @APIResponse(description = "Bad request.", responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(description = "Unauthorized.", responseCode = "401"),
            @APIResponse(description = "Forbidden.", responseCode = "403"),
            @APIResponse(description = "Not found.", responseCode = "404", content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(description = "Internal error.", responseCode = "500", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    @Operation(summary = "Get a Bridge instance", description = "Get a Bridge instance of the authenticated user by ID.")
    @GET
    @Path("{bridgeId}")
    public Response getBridge(@PathParam("bridgeId") @NotEmpty String bridgeId) {
        Bridge bridge = bridgesService.getBridge(bridgeId, identityResolver.resolve(jwt));
        return Response.ok(bridgesService.toResponse(bridge)).build();
    }

    @APIResponses(value = {
            @APIResponse(description = "Accepted.", responseCode = "202"),
            @APIResponse(description = "Bad request.", responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(description = "Unauthorized.", responseCode = "401"),
            @APIResponse(description = "Forbidden.", responseCode = "403"),
            @APIResponse(description = "Not found.", responseCode = "404", content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(description = "Internal error.", responseCode = "500", content = @Content(mediaType = MediaType.APPLICATION_JSON))
    })
    @Operation(summary = "Delete a Bridge instance", description = "Delete a Bridge instance of the authenticated user by ID.")
    @DELETE
    @Path("{bridgeId}")
    public Response deleteBridge(@PathParam("bridgeId") String bridgeId) {
        bridgesService.deleteBridge(bridgeId, identityResolver.resolve(jwt));
        return Response.accepted().build();
    }
}
