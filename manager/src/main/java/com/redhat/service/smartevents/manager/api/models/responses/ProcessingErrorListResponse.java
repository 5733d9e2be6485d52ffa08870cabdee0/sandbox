package com.redhat.service.smartevents.manager.api.models.responses;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.redhat.service.smartevents.infra.api.models.responses.PagedListResponse;

@Schema
public class ProcessingErrorListResponse extends PagedListResponse<ProcessingErrorResponse> {

    public ProcessingErrorListResponse() {
        super("ProcessingErrorList");
    }

}
