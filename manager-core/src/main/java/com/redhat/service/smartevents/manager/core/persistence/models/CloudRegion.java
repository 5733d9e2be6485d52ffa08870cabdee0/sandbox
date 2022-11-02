package com.redhat.service.smartevents.manager.core.persistence.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CloudRegion {

    @JsonProperty("name")
    private String name;

    @JsonProperty("display_name")
    private String displayName;

    @JsonProperty("enabled")
    private boolean enabled;

    public CloudRegion() {

    }

    public CloudRegion(String name, String displayName, boolean enabled) {
        this.name = name;
        this.displayName = displayName;
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
