package com.redhat.service.smartevents.manager.core.api.user;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.ExternalUserException;
import com.redhat.service.smartevents.infra.core.models.queries.QueryPageInfo;
import com.redhat.service.smartevents.infra.core.models.responses.ErrorsResponse;
import com.redhat.service.smartevents.infra.core.models.responses.PagedListResponse;
import com.redhat.service.smartevents.manager.core.api.models.responses.CloudProviderListResponse;
import com.redhat.service.smartevents.manager.core.api.models.responses.CloudProviderResponse;
import com.redhat.service.smartevents.manager.core.api.models.responses.CloudRegionListResponse;
import com.redhat.service.smartevents.manager.core.api.models.responses.CloudRegionResponse;
import com.redhat.service.smartevents.manager.core.persistence.dao.CloudProviderDAO;
import com.redhat.service.smartevents.manager.core.persistence.models.CloudProvider;

public abstract class AbstractCloudProviderAPI {

    private String cloudProvidersBasePath;
    private CloudProviderDAO cloudProviderDAO;

    public AbstractCloudProviderAPI() {
        // For CDI
    }

    public AbstractCloudProviderAPI(String cloudProvidersBasePath, CloudProviderDAO cloudProviderDAO) {
        this.cloudProvidersBasePath = cloudProvidersBasePath;
        this.cloudProviderDAO = cloudProviderDAO;
    }

    @APIResponses(value = {
            @APIResponse(description = "Success.", responseCode = "200",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CloudProviderListResponse.class))),
            @APIResponse(description = "Bad request.", responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorsResponse.class))),
            @APIResponse(description = "Internal error.", responseCode = "500", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorsResponse.class)))
    })
    @Operation(summary = "List Supported Cloud Providers.", description = "Returns the list of supported Cloud Providers.")
    @GET
    public Response listCloudProviders(@Valid @BeanParam QueryPageInfo queryInfo) {
        return Response.ok(PagedListResponse.fill(cloudProviderDAO.list(queryInfo), new CloudProviderListResponse(), cp -> CloudProviderResponse.from(cp, buildCloudProviderHref(cp)))).build();
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
            throw getItemNotFoundException("Cloud Provider with id '" + id + "' does not exist.");
        }

        return Response.ok(CloudProviderResponse.from(cloudProvider, buildCloudProviderHref(cloudProvider))).build();
    }

    protected abstract ExternalUserException getItemNotFoundException(String message);

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

    private String buildCloudProviderHref(CloudProvider cp) {
        return this.cloudProvidersBasePath + "/" + cp.getId();
    }
}
