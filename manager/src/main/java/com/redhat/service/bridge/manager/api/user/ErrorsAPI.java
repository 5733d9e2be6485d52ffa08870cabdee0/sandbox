package com.redhat.service.bridge.manager.api.user;

import java.util.Optional;

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
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.security.SecuritySchemes;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.redhat.service.bridge.infra.api.models.responses.ErrorListResponse;
import com.redhat.service.bridge.infra.api.models.responses.ErrorResponse;
import com.redhat.service.bridge.infra.api.models.responses.ListResponse;
import com.redhat.service.bridge.infra.exceptions.BridgeError;
import com.redhat.service.bridge.infra.exceptions.BridgeErrorService;
import com.redhat.service.bridge.infra.models.QueryInfo;

import io.quarkus.security.Authenticated;

import static com.redhat.service.bridge.infra.api.APIConstants.ERROR_API_BASE_PATH;

@SecuritySchemes(value = {
        @SecurityScheme(securitySchemeName = "bearer",
                type = SecuritySchemeType.HTTP,
                scheme = "Bearer")
})
@SecurityRequirement(name = "bearer")
@Path(ERROR_API_BASE_PATH)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "Errors")
public class ErrorsAPI {

    @Inject
    BridgeErrorService service;

    @GET
    @APIResponse(
            responseCode = "200",
            description = "The list of ErrorResponses",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorListResponse.class)))
    public Response getErrors(@Valid @BeanParam QueryInfo queryInfo) {
        return Response.ok(ListResponse.fill(service.getUserErrors(queryInfo), new ErrorListResponse(), ErrorResponse::from)).build();
    }

    @GET
    @Path("{id}")
    @APIResponses(
            value = {
                    @APIResponse(
                            responseCode = "200",
                            description = "The ErrorResponse with given ID",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(
                                            implementation = ErrorResponse.class))),
                    @APIResponse(
                            responseCode = "404",
                            description = "The given Error not found")
            })
    public Response getError(@PathParam("id") int id) {
        Optional<BridgeError> error = service.getUserError(id);
        return (error.isPresent() ? Response.ok(ErrorResponse.from(error.get())) : Response.status(Status.NOT_FOUND)).build();
    }
}
