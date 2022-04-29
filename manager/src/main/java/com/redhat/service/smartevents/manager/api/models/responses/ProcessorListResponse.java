package com.redhat.service.smartevents.manager.api.models.responses;

import com.redhat.service.smartevents.infra.api.models.responses.ListResponse;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema
public class ProcessorListResponse extends ListResponse<ProcessorResponse> {

    public ProcessorListResponse() {
        super("ProcessorList");
    }
}
