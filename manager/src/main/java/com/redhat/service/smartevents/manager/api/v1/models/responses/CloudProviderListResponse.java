package com.redhat.service.smartevents.manager.api.v1.models.responses;

import com.redhat.service.smartevents.infra.models.responses.PagedListResponse;

public class CloudProviderListResponse extends PagedListResponse<CloudProviderResponse> {

    private static final String KIND = "CloudProviderList";

    public CloudProviderListResponse() {
        super(KIND);
    }
}
