package com.redhat.service.rhose.manager.api.models.responses;

import com.redhat.service.rhose.infra.api.models.responses.ListResponse;

public class BridgeListResponse extends ListResponse<BridgeResponse> {

    public BridgeListResponse() {
        super("BridgeList");
    }

}
