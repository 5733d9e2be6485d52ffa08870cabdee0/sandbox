package com.redhat.service.smartevents.manager.v2.api.user.models.requests;

import com.redhat.service.smartevents.manager.core.api.models.requests.AbstractBridgeRequest;
import com.redhat.service.smartevents.manager.core.api.validators.ValidCloudProvider;

@ValidCloudProvider
// TODO: rename this class when https://github.com/redhat-developer/app-services-api-guidelines/issues/120 is fixed or when V1 is dropped.
public class BridgeRequestV2DTO extends AbstractBridgeRequest {

    public BridgeRequestV2DTO() {
    }

    public BridgeRequestV2DTO(String name, String cloudProvider, String region) {
        super(name, cloudProvider, region);
    }
}
