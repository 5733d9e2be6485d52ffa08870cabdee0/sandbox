package com.redhat.service.smartevents.manager.v2.api.user.models.requests;

import com.redhat.service.smartevents.manager.core.api.models.requests.AbstractBridgeRequest;
import com.redhat.service.smartevents.manager.core.api.validators.ValidCloudProvider;

@ValidCloudProvider
public class BridgeRequest extends AbstractBridgeRequest {

    public BridgeRequest() {
    }

    public BridgeRequest(String name, String cloudProvider, String region) {
        super(name, cloudProvider, region);
    }
}
