package com.redhat.service.smartevents.manager.core.api.models.responses;

import com.redhat.service.smartevents.infra.core.models.responses.PagedListResponse;

public class CloudProviderListResponse extends PagedListResponse<CloudProviderResponse> {

    private static final String KIND = "CloudProviderList";

    public CloudProviderListResponse() {
        super(KIND);
    }
}
