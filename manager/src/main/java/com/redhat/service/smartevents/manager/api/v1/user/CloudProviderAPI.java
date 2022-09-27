package com.redhat.service.smartevents.manager.api.v1.user;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.redhat.service.smartevents.infra.api.APIConstants;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.infra.models.queries.QueryPageInfo;
import com.redhat.service.smartevents.infra.models.responses.ErrorsResponse;
import com.redhat.service.smartevents.infra.models.responses.PagedListResponse;
import com.redhat.service.smartevents.manager.api.v1.models.responses.CloudProviderListResponse;
import com.redhat.service.smartevents.manager.api.v1.models.responses.CloudProviderResponse;
import com.redhat.service.smartevents.manager.api.v1.models.responses.CloudRegionListResponse;
import com.redhat.service.smartevents.manager.api.v1.models.responses.CloudRegionResponse;
import com.redhat.service.smartevents.manager.persistence.v1.dao.CloudProviderDAO;
import com.redhat.service.smartevents.manager.persistence.v1.models.CloudProvider;

@Tag(name = "Cloud Providers", description = "List Supported Cloud Providers and Regions")
@Path(APIConstants.V1_CLOUD_PROVIDERS_BASE_PATH)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RegisterRestClient
public class CloudProviderAPI {

    @Inject
    CloudProviderDAO cloudProviderDAO;

    @APIResponses(value = {
            @APIResponse(description = "Success.", responseCode = "200",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CloudProviderListResponse.class))),
            @APIResponse(description = "Bad request.", responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorsResponse.class))),
            @APIResponse(description = "Internal error.", responseCode = "500", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorsResponse.class)))
    })
    @Operation(summary = "List Supported Cloud Providers.", description = "Returns the list of supported Cloud Providers.")
    @GET
    public Response listCloudProviders(@Valid @BeanParam QueryPageInfo queryInfo) {
        return Response.ok(PagedListResponse.fill(cloudProviderDAO.list(queryInfo), new CloudProviderListResponse(), CloudProviderResponse::from)).build();
    }

    @APIResponses(value = {
            @APIResponse(description = "Success.", responseCode = "200",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CloudProviderListResponse.class))),
            @APIResponse(description = "Bad request.", responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorsResponse.class))),
            @APIResponse(description = "Not found.", responseCode = "404", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorsResponse.class))),
            @APIResponse(description = "Internal error.", responseCode = "500", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorsResponse.class)))
    })
    @Operation(summary = "Get Cloud Provider.", description = "Get details of the Cloud Provider specified by id.")
    @GET
    @Path("{id}")
    public Response getCloudProvider(@PathParam("id") @NotEmpty String id) {

        CloudProvider cloudProvider = cloudProviderDAO.findById(id);
        if (cloudProvider == null) {
            throw new ItemNotFoundException("Cloud Provider with id '" + id + "' does not exist.");
        }

        return Response.ok(CloudProviderResponse.from(cloudProvider)).build();
    }

    @APIResponses(value = {
            @APIResponse(description = "Success.", responseCode = "200",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CloudRegionListResponse.class))),
            @APIResponse(description = "Bad request.", responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorsResponse.class))),
            @APIResponse(description = "Internal error.", responseCode = "500", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorsResponse.class)))
    })
    @Operation(summary = "List Supported Cloud Regions.", description = "Returns the list of supported Regions of the specified Cloud Provider.")
    @GET
    @Path("{id}/regions")
    public Response listCloudProviderRegions(@PathParam("id") @NotEmpty String id, @Valid @BeanParam QueryPageInfo queryInfo) {
        return Response.ok(PagedListResponse.fill(cloudProviderDAO.listRegionsById(id, queryInfo), new CloudRegionListResponse(), CloudRegionResponse::from)).build();
    }
}
