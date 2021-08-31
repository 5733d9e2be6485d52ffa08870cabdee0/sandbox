package com.redhat.developer.manager.models;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.developer.infra.dto.ConnectorDTO;

@NamedQueries({
        @NamedQuery(name = "CONNECTOR.findByStatus",
                query = "from Connector where status=:status"),
        @NamedQuery(name = "CONNECTOR.findByNameAndCustomerId",
                query = "from Connector where name=:name and customerId=:customerId"),
})
@Entity
@Table(name = "CONNECTOR", uniqueConstraints = { @UniqueConstraint(columnNames = { "name", "customerId" }) })
public class Connector {

    @Id
    @JsonProperty("id")
    private String id = UUID.randomUUID().toString();

    @Column(name = "name")
    @JsonProperty("name")
    private String name;

    @Column(name = "endpoint")
    @JsonProperty("endpoint")
    private String endpoint;

    @Column(name = "customerId")
    @JsonProperty("customerId")
    private String customerId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    @JsonProperty("status")
    private ConnectorStatus status;

    public Connector() {
    }

    public Connector(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public ConnectorStatus getStatus() {
        return status;
    }

    public void setStatus(ConnectorStatus status) {
        this.status = status;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public ConnectorDTO toDTO() {
        ConnectorDTO dto = new ConnectorDTO();
        dto.setId(id);
        dto.setName(name);
        dto.setEndpoint(endpoint);
        dto.setStatus(status.toDTO());
        dto.setCustomerId(customerId);

        return dto;
    }

    public static Connector fromDTO(ConnectorDTO dto) {
        Connector connector = new Connector();
        connector.setId(dto.getId());
        connector.setEndpoint(dto.getEndpoint());
        connector.setCustomerId(dto.getCustomerId());
        connector.setStatus(ConnectorStatus.fromDTO(dto.getStatus()));

        return connector;
    }
}
