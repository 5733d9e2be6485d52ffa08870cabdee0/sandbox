package com.redhat.service.smartevents.manager.api.models.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.manager.models.CloudRegion;

public class CloudRegionResponse {

    @JsonProperty
    private final String kind = "CloudRegion";

    @JsonProperty
    private String id;

    @JsonProperty
    private String name;

    @JsonProperty("display_name")
    private String displayName;

    @JsonProperty
    private boolean enabled;

    public CloudRegionResponse() {

    }

    public CloudRegionResponse(String id, String name, String displayName, boolean enabled) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.enabled = enabled;
    }

    public String getKind() {
        return kind;
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

    public boolean isEnabled() {
        return enabled;
    }

    public static CloudRegionResponse from(CloudRegion cr) {
        return new CloudRegionResponse(cr.getId(), cr.getName(), cr.getDisplayName(), cr.isEnabled());
    }
}
