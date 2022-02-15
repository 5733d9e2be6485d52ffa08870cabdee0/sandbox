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
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.security.SecuritySchemes;

import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.api.models.responses.ListResponse;
import com.redhat.service.bridge.infra.auth.IdentityResolver;
import com.redhat.service.bridge.infra.models.QueryInfo;
import com.redhat.service.bridge.manager.BridgesService;
import com.redhat.service.bridge.manager.api.models.requests.BridgeRequest;
import com.redhat.service.bridge.manager.api.models.responses.BridgeListResponse;
import com.redhat.service.bridge.manager.models.Bridge;

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
public class BridgesAPI {

    @Inject
    IdentityResolver identityResolver;

    @Inject
    BridgesService bridgesService;

    @Inject
    JsonWebToken jwt;

    @GET
    public Response getBridges(@Valid @BeanParam QueryInfo queryInfo) {
        System.out.println(jwt.getSubject());
        return Response.ok(ListResponse.fill(bridgesService
                .getBridges(identityResolver.resolve(jwt), queryInfo), new BridgeListResponse(), bridgesService::toResponse)).build();
    }

    @POST
    public Response createBridge(BridgeRequest bridgeRequest) {
        Bridge bridge = bridgesService.createBridge(identityResolver.resolve(jwt), bridgeRequest);
        return Response.status(Response.Status.CREATED).entity(bridgesService.toResponse(bridge)).build();
    }

    @GET
    @Path("{id}")
    public Response getBridge(@PathParam("id") @NotEmpty String id) {
        Bridge bridge = bridgesService.getBridge(id, identityResolver.resolve(jwt));
        return Response.ok(bridgesService.toResponse(bridge)).build();
    }

    @DELETE
    @Path("{id}")
    public Response deleteBridge(@PathParam("id") String id) {
        bridgesService.deleteBridge(id, identityResolver.resolve(jwt));
        return Response.accepted().build();
    }
}
