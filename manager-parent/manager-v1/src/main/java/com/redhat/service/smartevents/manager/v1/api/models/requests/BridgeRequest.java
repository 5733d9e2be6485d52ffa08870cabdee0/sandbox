package com.redhat.service.smartevents.manager.v1.api.models.requests;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.v1.api.models.gateways.Action;
import com.redhat.service.smartevents.manager.core.api.models.requests.AbstractBridgeRequest;
import com.redhat.service.smartevents.manager.core.api.validators.ValidCloudProvider;
import com.redhat.service.smartevents.manager.v1.api.user.validators.processors.ValidErrorHandler;
import com.redhat.service.smartevents.manager.v1.persistence.models.Bridge;

@ValidCloudProvider
@ValidErrorHandler
public class BridgeRequest extends AbstractBridgeRequest {

    @JsonProperty("error_handler")
    @Valid
    protected Action errorHandler;

    public BridgeRequest() {
    }

    public BridgeRequest(String name, String cloudProvider, String region) {
        super(name, cloudProvider, region);
    }

    public BridgeRequest(String name, String cloudProvider, String region, Action errorHandler) {
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
