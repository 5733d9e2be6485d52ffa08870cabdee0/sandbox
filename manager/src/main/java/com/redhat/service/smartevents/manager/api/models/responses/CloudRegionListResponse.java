package com.redhat.service.smartevents.manager.api.models.responses;

import com.redhat.service.smartevents.infra.api.models.responses.PagedListResponse;

public class CloudRegionListResponse extends PagedListResponse<CloudRegionResponse> {

    private static final String KIND = "CloudRegionList";

    public CloudRegionListResponse() {
        super(KIND);
    }
}
