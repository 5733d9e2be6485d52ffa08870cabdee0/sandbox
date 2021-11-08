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
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.security.SecuritySchemes;

import com.redhat.service.bridge.manager.ErrorsService;
import com.redhat.service.bridge.manager.api.models.responses.ErrorListResponse;
import com.redhat.service.bridge.manager.api.models.responses.ErrorResponse;
import com.redhat.service.bridge.manager.api.models.responses.ListResponse;
import com.redhat.service.bridge.manager.models.Error;
import com.redhat.service.bridge.manager.models.QueryInfo;

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
public class ErrorsAPI {

    @Inject
    private ErrorsService service;

    @GET
    public Response getErrors(@Valid @BeanParam QueryInfo queryInfo) {
        return Response.ok(ListResponse.fill(service.getErrors(queryInfo), new ErrorListResponse(), ErrorResponse::from)).build();
    }

    @GET
    @Path("{id}")
    public Response getError(@PathParam("id") int id) {
        Optional<Error> error = service.getError(id);
        return (error.isPresent() ? Response.ok(ErrorResponse.from(error.get())) : Response.status(Status.NOT_FOUND)).build();
    }
}
