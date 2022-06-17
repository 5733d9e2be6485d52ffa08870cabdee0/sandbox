package com.redhat.service.smartevents.manager.api.models.responses;

import com.redhat.service.smartevents.infra.api.models.responses.ListResponse;

public class CloudRegionListResponse extends ListResponse<CloudRegionResponse> {

    private static final String KIND = "CloudRegionList";

    public CloudRegionListResponse() {
        super(KIND);
    }
}
