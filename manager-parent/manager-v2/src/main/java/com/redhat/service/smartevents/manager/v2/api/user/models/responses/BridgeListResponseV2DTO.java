package com.redhat.service.smartevents.manager.v2.api.user.models.responses;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.redhat.service.smartevents.infra.core.models.responses.PagedListResponse;

@Schema
// TODO: rename this class when https://github.com/redhat-developer/app-services-api-guidelines/issues/120 is fixed or when V1 is dropped.
public class BridgeListResponseV2DTO extends PagedListResponse<BridgeResponseV2DTO> {

    public BridgeListResponseV2DTO() {
        super("BridgeList");
    }

}
