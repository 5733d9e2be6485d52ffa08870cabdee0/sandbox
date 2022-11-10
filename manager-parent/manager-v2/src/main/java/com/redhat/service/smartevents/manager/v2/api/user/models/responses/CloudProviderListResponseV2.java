package com.redhat.service.smartevents.manager.v2.api.user.models.responses;

import com.redhat.service.smartevents.infra.core.models.responses.PagedListResponse;

public class CloudProviderListResponseV2 extends PagedListResponse<CloudProviderResponseV2> {

    private static final String KIND = "CloudProviderList";

    public CloudProviderListResponseV2() {
        super(KIND);
    }
}
