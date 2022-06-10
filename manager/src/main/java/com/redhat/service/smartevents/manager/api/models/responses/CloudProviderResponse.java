package com.redhat.service.smartevents.manager.api.models.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.manager.models.CloudProvider;

public class CloudProviderResponse {

    @JsonProperty
    private final String kind = "CloudProvider";

    @JsonProperty
    String id;

    @JsonProperty
    String name;

    @JsonProperty("display_name")
    String displayName;

    @JsonProperty
    boolean enabled;

    CloudProviderResponse() {

    }

    CloudProviderResponse(String id, String name, String displayName, boolean enabled) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.enabled = enabled;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getKind() {
        return kind;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public static CloudProviderResponse from(CloudProvider cp) {
        return new CloudProviderResponse(cp.getId(), cp.getName(), cp.getDisplayName(), cp.isEnabled());
    }
}
