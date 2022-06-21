package com.redhat.service.smartevents.infra.api.models.responses;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema
public class ErrorsResponse extends ListResponse<ErrorResponse> {

    public ErrorsResponse() {
        super("Errors");
    }

}
