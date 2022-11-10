package com.redhat.service.smartevents.manager.v1.api.models.requests;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.v1.api.models.gateways.Action;
import com.redhat.service.smartevents.manager.core.api.models.requests.BridgeRequest;
import com.redhat.service.smartevents.manager.core.api.validators.ValidCloudProvider;
import com.redhat.service.smartevents.manager.v1.api.user.validators.processors.ValidErrorHandler;
import com.redhat.service.smartevents.manager.v1.persistence.models.Bridge;

@ValidCloudProvider
@ValidErrorHandler
public class BridgeRequestV1 extends BridgeRequest {

    @JsonProperty("error_handler")
    @Valid
    protected Action errorHandler;

    public BridgeRequestV1() {
    }

    public BridgeRequestV1(String name, String cloudProvider, String region) {
        super(name, cloudProvider, region);
    }

    public BridgeRequestV1(String name, String cloudProvider, String region, Action errorHandler) {
        this(name, cloudProvider, region);
        this.errorHandler = errorHandler;
    }

    public Bridge toEntity() {
        return new Bridge(getName());
    }

    public Action getErrorHandler() {
        return errorHandler;
    }
}
