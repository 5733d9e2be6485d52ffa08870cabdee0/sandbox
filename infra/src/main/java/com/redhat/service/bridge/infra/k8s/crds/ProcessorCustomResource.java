package com.redhat.service.bridge.infra.k8s.crds;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.bridge.infra.models.actions.ActionRequest;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.infra.models.filters.BaseFilter;

// TODO: move to shard or shard-api. It is in this infra module because k8s module needs it atm
public class ProcessorCustomResource {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("bridge")
    private BridgeDTO bridge;

    @JsonProperty("status")
    private BridgeStatus status;

    @JsonProperty("filters")
    private Set<BaseFilter> filters;

    @JsonProperty("action")
    private ActionRequest action;

    public ProcessorCustomResource() {
    }

    public ProcessorCustomResource(String id, String name, BridgeDTO bridge, BridgeStatus status, Set<BaseFilter> filters, ActionRequest action) {
        this.id = id;
        this.name = name;
        this.bridge = bridge;
        this.status = status;
        this.filters = filters;
        this.action = action;
    }

    public ActionRequest getAction() {
        return action;
    }

    public void setAction(ActionRequest action) {
        this.action = action;
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

    public Set<BaseFilter> getFilters() {
        return filters;
    }

    public void setFilters(Set<BaseFilter> filters) {
        this.filters = filters;
    }

    public static ProcessorCustomResource fromDTO(ProcessorDTO dto) {
        ProcessorCustomResource resource = new ProcessorCustomResource();
        resource.setId(dto.getId());
        resource.setName(dto.getName());
        resource.setBridge(dto.getBridge());
        resource.setStatus(dto.getStatus());
        resource.setFilters(dto.getFilters());
        resource.setAction(dto.getAction());

        return resource;
    }

    public ProcessorDTO toDTO() {
        ProcessorDTO dto = new ProcessorDTO();
        dto.setId(id);
        dto.setName(name);
        dto.setBridge(bridge);
        dto.setStatus(status);
        dto.setFilters(filters);
        dto.setAction(this.action);

        return dto;
    }
}
