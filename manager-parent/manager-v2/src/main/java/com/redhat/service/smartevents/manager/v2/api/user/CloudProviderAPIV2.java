package com.redhat.service.smartevents.manager.v2.api.user;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.redhat.service.smartevents.infra.v2.api.V2APIConstants;
import com.redhat.service.smartevents.manager.core.api.user.AbstractCloudProviderAPI;

@Tag(name = "Cloud Providers", description = "List Supported Cloud Providers and Regions")
@Path(V2APIConstants.V2_CLOUD_PROVIDERS_BASE_PATH)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RegisterRestClient
public class CloudProviderAPIV2 extends AbstractCloudProviderAPI {

    public CloudProviderAPIV2() {
        super(V2APIConstants.V2_CLOUD_PROVIDERS_BASE_PATH);
    }
}
