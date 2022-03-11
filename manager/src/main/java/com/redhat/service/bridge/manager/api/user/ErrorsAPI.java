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
import com.redhat.service.bridge.infra.api.models.responses.ErrorListResponse;
import com.redhat.service.bridge.infra.api.models.responses.ErrorResponse;
import com.redhat.service.bridge.infra.api.models.responses.ListResponse;
import com.redhat.service.bridge.infra.exceptions.BridgeError;
import com.redhat.service.bridge.infra.exceptions.BridgeErrorService;
import com.redhat.service.bridge.infra.models.QueryInfo;

import io.quarkus.security.Authenticated;

import static com.redhat.service.bridge.infra.api.APIConstants.ERROR_API_BASE_PATH;

@Tag(name = "Errors Catalog API", description = "List and get the error definitions from the error catalog.")
@SecuritySchemes(value = {
        @SecurityScheme(securitySchemeName = "bearer",
                type = SecuritySchemeType.HTTP,
                scheme = "Bearer")
})
@SecurityRequirement(name = "bearer")
@Path(APIConstants.ERROR_API_BASE_PATH)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class ErrorsAPI {

    @Inject
    BridgeErrorService service;

    @APIResponses(value = {
            @APIResponse(description = "Success.", responseCode = "200",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = SchemaType.OBJECT, implementation = ErrorListResponse.class))),
            @APIResponse(description = "Bad request.", responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(description = "Unauthorized.", responseCode = "401")
    })
    @Operation(summary = "Get the list of errors.", description = "Get the list of errors from the error catalog.")
    @GET
    public Response getErrors(@Valid @BeanParam QueryInfo queryInfo) {
        return Response.ok(ListResponse.fill(service.getUserErrors(queryInfo), new ErrorListResponse(), ErrorResponse::from)).build();
    }

    @APIResponses(value = {
            @APIResponse(description = "Success.", responseCode = "200",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(type = SchemaType.OBJECT, implementation = BridgeError.class))),
            @APIResponse(description = "Bad request.", responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_JSON)),
            @APIResponse(description = "Unauthorized.", responseCode = "401")
    })
    @Operation(summary = "Get an error from the error catalog.", description = "Get an error from the error catalog.")
    @GET
    @Path("{id}")
    public Response getError(@PathParam("id") int id) {
        Optional<BridgeError> error = service.getUserError(id);
        return (error.isPresent() ? Response.ok(ErrorResponse.from(error.get())) : Response.status(Status.NOT_FOUND)).build();
    }
}
