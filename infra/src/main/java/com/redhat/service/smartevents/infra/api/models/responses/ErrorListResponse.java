package com.redhat.service.smartevents.infra.api.models.responses;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema
public class ErrorListResponse extends ListResponse<ErrorResponse> {

    public ErrorListResponse() {
        super("ErrorList");
    }

}
