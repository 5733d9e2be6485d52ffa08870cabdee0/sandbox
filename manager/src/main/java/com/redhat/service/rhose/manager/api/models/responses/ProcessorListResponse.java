package com.redhat.service.rhose.manager.api.models.responses;

import com.redhat.service.rhose.infra.api.models.responses.ListResponse;

public class ProcessorListResponse extends ListResponse<ProcessorResponse> {

    public ProcessorListResponse() {
        super("ProcessorList");
    }
}
