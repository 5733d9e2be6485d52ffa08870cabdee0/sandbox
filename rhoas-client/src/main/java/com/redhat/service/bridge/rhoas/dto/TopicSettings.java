package com.redhat.service.bridge.rhoas.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TopicSettings {

    private int numPartitions;
    private List<ConfigEntry> config;

    public int getNumPartitions() {
        return numPartitions;
    }

    public void setNumPartitions(int numPartitions) {
        this.numPartitions = numPartitions;
    }

    public List<ConfigEntry> getConfig() {
        return config;
    }

    public void setConfig(List<ConfigEntry> config) {
        this.config = config;
    }

    @Override
    public String toString() {
        return "TopicSettings{" +
                "numPartitions=" + numPartitions +
                ", config=" + config +
                '}';
    }

    public static TopicSettings defaultSettings() {
        TopicSettings ts = new TopicSettings();
        ts.setNumPartitions(1);
        return ts;
    }
}
