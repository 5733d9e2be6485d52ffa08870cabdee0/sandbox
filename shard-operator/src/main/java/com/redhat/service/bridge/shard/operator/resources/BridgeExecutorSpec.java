package com.redhat.service.bridge.shard.operator.resources;

import com.redhat.service.bridge.infra.models.dto.BridgeDTO;

public class BridgeExecutorSpec {
    private String image;

    private String id;

    private BridgeDTO bridgeDTO;

    private String processorName;

    private String processorDefinition;

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public BridgeDTO getBridgeDTO() {
        return bridgeDTO;
    }

    public void setBridgeDTO(BridgeDTO bridgeDTO) {
        this.bridgeDTO = bridgeDTO;
    }

    public String getProcessorName() {
        return processorName;
    }

    public void setProcessorName(String processorName) {
        this.processorName = processorName;
    }

    public String getProcessorDefinition() {
        return processorDefinition;
    }

    public void setProcessorDefinition(String processorDefinition) {
        this.processorDefinition = processorDefinition;
    }
}
