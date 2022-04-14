package com.redhat.service.smartevents.manager.api.models.responses;

import com.redhat.service.smartevents.infra.api.models.responses.ListResponse;

public class ProcessorListResponse extends ListResponse<ProcessorResponse> {

    public ProcessorListResponse() {
        super("ProcessorList");
    }
}
