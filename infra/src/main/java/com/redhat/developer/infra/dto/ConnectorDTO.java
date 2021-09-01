package com.redhat.developer.infra.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ConnectorDTO {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("endpoint")
    private String endpoint;

    @JsonProperty("customerId")
    private String customerId;

    @JsonProperty("status")
    private ConnectorStatus status;

    public ConnectorDTO() {
    }

    public ConnectorDTO(String id, String name, String endpoint, String customerId, ConnectorStatus status) {
        this.id = id;
        this.name = name;
        this.endpoint = endpoint;
        this.customerId = customerId;
        this.status = status;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public void setStatus(ConnectorStatus status) {
        this.status = status;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getName() {
        return name;
    }

    public ConnectorStatus getStatus() {
        return status;
    }
}
