package com.redhat.service.smartevents.manager.v2.api.user.models.responses;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.redhat.service.smartevents.infra.core.models.responses.PagedListResponse;

@Schema
public class SourceConnectorListResponse extends PagedListResponse<SourceConnectorResponse> {

    public SourceConnectorListResponse() {
        super("SourceConnectorList");
    }
}
