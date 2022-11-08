package com.redhat.service.smartevents.manager.core.api.models.responses;

import com.redhat.service.smartevents.infra.core.models.responses.PagedListResponse;

public class CloudRegionListResponse extends PagedListResponse<CloudRegionResponse> {

    private static final String KIND = "CloudRegionList";

    public CloudRegionListResponse() {
        super(KIND);
    }
}
