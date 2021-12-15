package com.redhat.service.bridge.rhoas.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Topic {

    private String name;
    private Boolean isInternal;
    private List<ConfigEntry> config;
    private List<Partition> partitions;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getInternal() {
        return isInternal;
    }

    public void setInternal(Boolean internal) {
        isInternal = internal;
    }

    public List<ConfigEntry> getConfig() {
        return config;
    }

    public void setConfig(List<ConfigEntry> config) {
        this.config = config;
    }

    public List<Partition> getPartitions() {
        return partitions;
    }

    public void setPartitions(List<Partition> partitions) {
        this.partitions = partitions;
    }

    @Override
    public String toString() {
        return "Topic{" +
                "name='" + name + '\'' +
                ", isInternal=" + isInternal +
                ", config=" + config +
                ", partitions=" + partitions +
                '}';
    }
}
