package com.redhat.service.smartevents.manager.api.v1.models.responses;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.redhat.service.smartevents.infra.models.responses.PagedListResponse;

@Schema
public class ProcessorListResponse extends PagedListResponse<ProcessorResponse> {

    public ProcessorListResponse() {
        super("ProcessorList");
    }
}
