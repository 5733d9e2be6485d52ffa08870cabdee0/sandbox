package com.redhat.service.smartevents.manager.v2.api.user;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.redhat.service.smartevents.infra.v2.api.V2APIConstants;
import com.redhat.service.smartevents.manager.core.api.user.AbstractErrorsAPI;

@Tag(name = "Error Catalog", description = "List and get the error definitions from the error catalog.")
@Path(V2APIConstants.V2_ERROR_API_BASE_PATH)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RegisterRestClient
public class ErrorsAPIV2 extends AbstractErrorsAPI {
    public ErrorsAPIV2() {
        super(V2APIConstants.V2_ERROR_API_BASE_PATH);
    }
}
