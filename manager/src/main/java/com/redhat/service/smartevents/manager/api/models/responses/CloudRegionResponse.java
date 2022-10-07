package com.redhat.service.smartevents.manager.api.models.responses;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.manager.models.CloudRegion;

public class CloudRegionResponse {

    @NotNull
    @JsonProperty("kind")
    private static final String kind = "CloudRegion";

    @NotNull
    @JsonProperty("name")
    private String name;

    @NotNull
    @JsonProperty("display_name")
    private String displayName;

    @NotNull
    @JsonProperty("enabled")
    private boolean enabled;

    public CloudRegionResponse() {

    }

    public CloudRegionResponse(String name, String displayName, boolean enabled) {
        this.name = name;
        this.displayName = displayName;
        this.enabled = enabled;
    }

    public String getKind() {
        return kind;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public static CloudRegionResponse from(CloudRegion cr) {
        return new CloudRegionResponse(cr.getName(), cr.getDisplayName(), cr.isEnabled());
    }
}
