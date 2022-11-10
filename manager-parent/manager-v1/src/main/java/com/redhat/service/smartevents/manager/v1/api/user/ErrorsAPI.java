package com.redhat.service.smartevents.manager.v1.api.user;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.redhat.service.smartevents.infra.v1.api.V1APIConstants;
import com.redhat.service.smartevents.manager.core.api.user.AbstractErrorsAPI;

@Tag(name = "Error Catalog", description = "List and get the error definitions from the error catalog.")
@Path(V1APIConstants.V1_ERROR_API_BASE_PATH)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RegisterRestClient
public class ErrorsAPI extends AbstractErrorsAPI {
    public ErrorsAPI() {
        super(V1APIConstants.V1_ERROR_API_BASE_PATH);
    }
}
