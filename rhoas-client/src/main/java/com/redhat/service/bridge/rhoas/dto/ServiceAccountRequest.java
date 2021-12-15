package com.redhat.service.bridge.rhoas.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceAccountRequest {

    private String name;
    private String description;

    public ServiceAccountRequest() {
    }

    public ServiceAccountRequest(String name) {
        this.name = name;
    }

    public ServiceAccountRequest(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "ServiceAccountRequest{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }

}
