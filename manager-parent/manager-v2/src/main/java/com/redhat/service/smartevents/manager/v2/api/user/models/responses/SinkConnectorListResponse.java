package com.redhat.service.smartevents.manager.v2.api.user.models.responses;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema
public class SinkConnectorListResponse extends ConnectorListResponse<SinkConnectorResponse> {

    public SinkConnectorListResponse() {
        super("SinkConnectorList");
    }
}
