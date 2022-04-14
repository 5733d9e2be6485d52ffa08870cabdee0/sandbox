package com.redhat.service.smartevents.manager.api.models.responses;

import com.redhat.service.smartevents.infra.api.models.responses.ListResponse;

public class BridgeListResponse extends ListResponse<BridgeResponse> {

    public BridgeListResponse() {
        super("BridgeList");
    }

}
