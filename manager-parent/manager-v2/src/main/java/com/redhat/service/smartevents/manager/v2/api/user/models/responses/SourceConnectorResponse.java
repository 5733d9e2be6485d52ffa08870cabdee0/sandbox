package com.redhat.service.smartevents.manager.v2.api.user.models.responses;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SourceConnectorResponse extends ConnectorResponse {

    public SourceConnectorResponse() {
        super("SourceConnector");
    }
}
