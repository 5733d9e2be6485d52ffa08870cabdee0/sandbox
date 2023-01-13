package com.redhat.service.smartevents.manager.v2.api.user;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.redhat.service.smartevents.infra.core.exceptions.BridgeErrorService;
import com.redhat.service.smartevents.infra.v2.api.V2;
import com.redhat.service.smartevents.infra.v2.api.V2APIConstants;
import com.redhat.service.smartevents.manager.core.api.user.AbstractErrorsAPI;

@Tag(name = "Error Catalog", description = "List and get the error definitions from the error catalog.")
@Path(V2APIConstants.V2_ERROR_API_BASE_PATH)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RegisterRestClient
public class ErrorsAPI extends AbstractErrorsAPI {

    public ErrorsAPI() {
        //CDI proxy
    }

    @Inject
    public ErrorsAPI(@V2 BridgeErrorService service) {
        super(V2APIConstants.V2_ERROR_API_BASE_PATH, service);
    }
}
