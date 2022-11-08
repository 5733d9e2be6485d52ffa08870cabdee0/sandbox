package com.redhat.service.smartevents.manager.v1.api.models.responses;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.redhat.service.smartevents.infra.core.models.responses.PagedListResponse;

@Schema
public class ProcessorListResponse extends PagedListResponse<ProcessorResponse> {

    public ProcessorListResponse() {
        super("ProcessorList");
    }
}
