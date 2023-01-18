package com.redhat.service.smartevents.manager.v2.api.user.models.responses;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema
public class SourceConnectorListResponse extends ConnectorListResponse<SourceConnectorResponse> {

    public SourceConnectorListResponse() {
        super("SourceConnectorList");
    }
}
