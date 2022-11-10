package com.redhat.service.smartevents.manager.core.api.user;

import java.util.Optional;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import com.redhat.service.smartevents.infra.core.exceptions.BridgeError;
import com.redhat.service.smartevents.infra.core.exceptions.BridgeErrorService;
import com.redhat.service.smartevents.infra.core.models.queries.QueryPageInfo;
import com.redhat.service.smartevents.infra.core.models.responses.ErrorListResponse;
import com.redhat.service.smartevents.infra.core.models.responses.ErrorResponse;
import com.redhat.service.smartevents.infra.core.models.responses.ErrorsResponse;
import com.redhat.service.smartevents.infra.core.models.responses.PagedListResponse;

public abstract class AbstractErrorsAPI {

    private String errorsBasePath;

    @Inject
    BridgeErrorService service;

    public AbstractErrorsAPI() {
        // For CDI
    }

    public AbstractErrorsAPI(String errorsBasePath) {
        this.errorsBasePath = errorsBasePath;
    }

    @APIResponses(value = {
            @APIResponse(description = "Success.", responseCode = "200",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorListResponse.class))),
            @APIResponse(description = "Bad request.", responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorsResponse.class))),
            @APIResponse(description = "Unauthorized.", responseCode = "401"),
            @APIResponse(description = "Internal error.", responseCode = "500", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorsResponse.class)))
    })
    @Operation(summary = "Get the list of errors.", description = "Get the list of errors from the error catalog.")
    @GET
    public Response getErrors(@Valid @BeanParam QueryPageInfo queryInfo) {
        return Response.ok(PagedListResponse.fill(service.getUserErrors(queryInfo), new ErrorListResponse(), this::from)).build();
    }

    @APIResponses(value = {
            @APIResponse(description = "Success.", responseCode = "200",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = BridgeError.class))),
            @APIResponse(description = "Bad request.", responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorsResponse.class))),
            @APIResponse(description = "Unauthorized.", responseCode = "401"),
            @APIResponse(description = "Internal error.", responseCode = "500", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorsResponse.class)))
    })
    @Operation(summary = "Get an error from the error catalog.", description = "Get an error from the error catalog.")
    @GET
    @Path("{id}")
    public Response getError(@PathParam("id") int id) {
        Optional<BridgeError> error = service.getUserError(id);
        return (error.isPresent() ? Response.ok(from(error.get())) : Response.status(Status.NOT_FOUND)).build();
    }

    private ErrorResponse from(BridgeError bridgeError) {
        ErrorResponse response = ErrorResponse.from(bridgeError);
        response.setHref(this.errorsBasePath + bridgeError.getId());
        return response;
    }
}
