package com.redhat.service.bridge.rhoas.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TopicRequest {

    private String name;
    private TopicSettings settings;

    public TopicRequest() {
    }

    public TopicRequest(String name) {
        this.name = name;
        this.settings = TopicSettings.defaultSettings();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TopicSettings getSettings() {
        return settings;
    }

    public void setSettings(TopicSettings settings) {
        this.settings = settings;
    }

    @Override
    public String toString() {
        return "TopicRequest{" +
                "name='" + name + '\'' +
                ", settings=" + settings +
                '}';
    }
}
