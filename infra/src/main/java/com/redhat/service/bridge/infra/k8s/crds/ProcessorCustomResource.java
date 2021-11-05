package com.redhat.service.bridge.infra.k8s.crds;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.infra.models.processors.ProcessorDefinition;

// TODO: move to shard or shard-api. It is in this infra module because k8s module needs it atm
public class ProcessorCustomResource {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("definition")
    private ProcessorDefinition definition;

    @JsonProperty("bridge")
    private BridgeDTO bridge;

    @JsonProperty("status")
    private BridgeStatus status;

    public ProcessorCustomResource() {
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

    public ProcessorDefinition getDefinition() {
        return definition;
    }

    public void setDefinition(ProcessorDefinition definition) {
        this.definition = definition;
    }

    public BridgeDTO getBridge() {
        return bridge;
    }

    public void setBridge(BridgeDTO bridge) {
        this.bridge = bridge;
    }

    public BridgeStatus getStatus() {
        return status;
    }

    public void setStatus(BridgeStatus status) {
        this.status = status;
    }

    public static ProcessorCustomResource fromDTO(ProcessorDTO dto) {
        ProcessorCustomResource resource = new ProcessorCustomResource();
        resource.setId(dto.getId());
        resource.setName(dto.getName());
        resource.setBridge(dto.getBridge());
        resource.setStatus(dto.getStatus());
        resource.setDefinition(dto.getDefinition());
        return resource;
    }

    public ProcessorDTO toDTO() {
        ProcessorDTO dto = new ProcessorDTO();
        dto.setId(id);
        dto.setName(name);
        dto.setBridge(bridge);
        dto.setStatus(status);
        dto.setDefinition(definition);
        return dto;
    }
}
