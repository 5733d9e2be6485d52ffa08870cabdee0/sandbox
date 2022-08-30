package com.redhat.service.smartevents.processingerrors.api.models;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.redhat.service.smartevents.infra.api.models.responses.PagedListResponse;

@Schema
public class ProcessingErrorListResponse extends PagedListResponse<ProcessingErrorResponse> {

    public static final String KIND = "ProcessingErrorList";

    public ProcessingErrorListResponse() {
        super(KIND);
    }

}
