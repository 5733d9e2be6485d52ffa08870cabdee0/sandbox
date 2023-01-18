package com.redhat.service.smartevents.manager.v2.api.user.models.responses;

import com.redhat.service.smartevents.infra.core.models.responses.PagedListResponse;

public abstract class ConnectorListResponse<T extends ConnectorResponse> extends PagedListResponse<T> {

    protected ConnectorListResponse(String kind) {
        super(kind);
    }
}
