package com.redhat.service.smartevents.manager.models;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CloudProvider {

    @JsonProperty("id")
    String id;

    @JsonProperty("name")
    String name;

    @JsonProperty("display_name")
    String displayName;

    @JsonProperty("enabled")
    boolean enabled;

    @JsonProperty("regions")
    private List<CloudRegion> regions;

    public CloudProvider() {

    }

    public CloudProvider(String id, String name, String displayName, boolean enabled, List<CloudRegion> regions) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.enabled = enabled;
        this.regions = regions;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public List<CloudRegion> getRegions() {
        return regions;
    }

    public void setRegions(List<CloudRegion> regions) {
        this.regions = regions;
    }

    @JsonIgnore
    public Optional<CloudRegion> getRegionByName(String name) {
        if (this.regions == null) {
            return Optional.empty();
        }
        return this.regions.stream().filter(r -> r.getName().equals(name)).findFirst();
    }
}
