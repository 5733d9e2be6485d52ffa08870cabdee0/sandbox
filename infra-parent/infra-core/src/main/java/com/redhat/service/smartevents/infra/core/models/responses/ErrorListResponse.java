package com.redhat.service.smartevents.infra.core.models.responses;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema
public class ErrorListResponse extends PagedListResponse<ErrorResponse> {

    public ErrorListResponse() {
        super("ErrorList");
    }

}
