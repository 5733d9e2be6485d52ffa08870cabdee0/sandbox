package com.redhat.service.bridge.rhoas.request;

public class ServiceAccountRequest {

    private String name;
    private String description;

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
