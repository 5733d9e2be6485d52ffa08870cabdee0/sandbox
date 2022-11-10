package com.redhat.service.smartevents.manager.v2.api.user.models.responses;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.core.models.responses.BaseResponse;
import com.redhat.service.smartevents.infra.v2.api.V2APIConstants;
import com.redhat.service.smartevents.manager.core.persistence.models.CloudProvider;

public class CloudProviderResponseV2 extends BaseResponse {

    private static final String KIND = "CloudProvider";

    @NotNull
    @JsonProperty("display_name")
    String displayName;

    @NotNull
    @JsonProperty("enabled")
    boolean enabled;

    CloudProviderResponseV2() {
        super(KIND);
    }

    CloudProviderResponseV2(String id, String name, String displayName, boolean enabled) {
        super(KIND);
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.enabled = enabled;
        this.href = V2APIConstants.V2_CLOUD_PROVIDERS_BASE_PATH + "/" + id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public static CloudProviderResponseV2 from(CloudProvider cp) {
        return new CloudProviderResponseV2(cp.getId(), cp.getName(), cp.getDisplayName(), cp.isEnabled());
    }
}
