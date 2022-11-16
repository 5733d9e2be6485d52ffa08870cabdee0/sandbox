package com.redhat.service.smartevents.manager.v2.api.user.models.responses;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.redhat.service.smartevents.infra.core.models.responses.PagedListResponse;

@Schema
public class BridgeListResponse extends PagedListResponse<BridgeResponse> {

    public BridgeListResponse() {
        super("BridgeList");
    }

}
