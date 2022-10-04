package com.redhat.service.smartevents.processingerrors.api;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
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
import com.redhat.service.smartevents.infra.api.models.responses.ErrorsResponse;
import com.redhat.service.smartevents.infra.api.models.responses.PagedListResponse;
import com.redhat.service.smartevents.infra.auth.IdentityResolver;
import com.redhat.service.smartevents.infra.models.ListResult;
import com.redhat.service.smartevents.infra.models.QueryResourceInfo;
import com.redhat.service.smartevents.processingerrors.ProcessingErrorService;
import com.redhat.service.smartevents.processingerrors.api.models.ProcessingErrorListResponse;
import com.redhat.service.smartevents.processingerrors.models.ProcessingError;

import io.quarkus.security.Authenticated;

@Tag(name = "Processing Errors", description = "The API that allow the user to retrieve processing errors of a specific bridge instance.")
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
public class ProcessingErrorsAPI {

    @Inject
    IdentityResolver identityResolver;

    @Inject
    ProcessingErrorService processingErrorService;

    @Inject
    JsonWebToken jwt;

    @APIResponses(value = {
            @APIResponse(description = "Success.", responseCode = "200",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProcessingErrorListResponse.class))),
            @APIResponse(description = "Bad request.", responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorsResponse.class))),
            @APIResponse(description = "Unauthorized.", responseCode = "401", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorsResponse.class))),
            @APIResponse(description = "Forbidden.", responseCode = "403", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorsResponse.class))),
            @APIResponse(description = "Not found.", responseCode = "404", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorsResponse.class))),
            @APIResponse(description = "Internal error.", responseCode = "500", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorsResponse.class)))
    })
    @Operation(hidden = true, summary = "Get the list of errors for a particular Bridge instance", description = "Get the list of errors for a particular Bridge instance for the authenticated user.")
    @GET
    @Path("{bridgeId}/errors")
    public Response getBridgeErrors(@PathParam("bridgeId") String bridgeId, @Valid @BeanParam QueryResourceInfo queryInfo) {
        ListResult<ProcessingError> errors = processingErrorService.getProcessingErrors(bridgeId, identityResolver.resolve(jwt), queryInfo);
        return Response.ok(PagedListResponse.fill(errors, new ProcessingErrorListResponse(), processingErrorService::toResponse)).build();
    }
}
