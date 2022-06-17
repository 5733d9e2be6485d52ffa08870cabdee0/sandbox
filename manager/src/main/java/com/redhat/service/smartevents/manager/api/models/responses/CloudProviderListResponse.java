package com.redhat.service.smartevents.manager.api.models.responses;

import com.redhat.service.smartevents.infra.api.models.responses.ListResponse;

public class CloudProviderListResponse extends ListResponse<CloudProviderResponse> {

    private static final String KIND = "CloudProviderList";

    public CloudProviderListResponse() {
        super(KIND);
    }
}
