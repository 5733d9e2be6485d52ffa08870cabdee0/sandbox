package com.redhat.service.smartevents.manager.v2.api.user;

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

import com.redhat.service.smartevents.manager.core.api.user.AbstractErrorsAPI;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.redhat.service.smartevents.infra.core.exceptions.BridgeError;
import com.redhat.service.smartevents.infra.core.exceptions.BridgeErrorService;
import com.redhat.service.smartevents.infra.core.models.queries.QueryPageInfo;
import com.redhat.service.smartevents.infra.core.models.responses.ErrorListResponse;
import com.redhat.service.smartevents.infra.core.models.responses.ErrorResponse;
import com.redhat.service.smartevents.infra.core.models.responses.ErrorsResponse;
import com.redhat.service.smartevents.infra.core.models.responses.PagedListResponse;
import com.redhat.service.smartevents.infra.v2.api.V2APIConstants;

@Tag(name = "Error Catalog", description = "List and get the error definitions from the error catalog.")
@Path(V2APIConstants.V2_ERROR_API_BASE_PATH)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RegisterRestClient
public class ErrorsAPIV2 extends AbstractErrorsAPI {
    public ErrorsAPIV2(){
        super(V2APIConstants.V2_ERROR_API_BASE_PATH);
    }
}

