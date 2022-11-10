package com.redhat.service.smartevents.manager.v1.api.user;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.redhat.service.smartevents.infra.v1.api.V1APIConstants;
import com.redhat.service.smartevents.manager.core.api.user.AbstractCloudProviderAPI;

@Tag(name = "Cloud Providers", description = "List Supported Cloud Providers and Regions")
@Path(V1APIConstants.V1_CLOUD_PROVIDERS_BASE_PATH)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RegisterRestClient
public class CloudProviderAPI extends AbstractCloudProviderAPI {

    public CloudProviderAPI() {
        super(V1APIConstants.V1_CLOUD_PROVIDERS_BASE_PATH);
    }
}
