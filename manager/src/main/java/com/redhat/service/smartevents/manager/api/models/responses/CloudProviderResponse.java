package com.redhat.service.smartevents.manager.api.models.responses;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.api.APIConstants;
import com.redhat.service.smartevents.infra.api.models.responses.BaseResponse;
import com.redhat.service.smartevents.manager.models.CloudProvider;

public class CloudProviderResponse extends BaseResponse {

    private static final String KIND = "CloudProvider";

    @NotNull
    @JsonProperty("display_name")
    String displayName;

    @NotNull
    @JsonProperty("enabled")
    boolean enabled;

    CloudProviderResponse() {
        super(KIND);
    }

    CloudProviderResponse(String id, String name, String displayName, boolean enabled) {
        super(KIND);
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.enabled = enabled;
        this.href = APIConstants.CLOUD_PROVIDERS_BASE_PATH + "/" + id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public static CloudProviderResponse from(CloudProvider cp) {
        return new CloudProviderResponse(cp.getId(), cp.getName(), cp.getDisplayName(), cp.isEnabled());
    }
}
