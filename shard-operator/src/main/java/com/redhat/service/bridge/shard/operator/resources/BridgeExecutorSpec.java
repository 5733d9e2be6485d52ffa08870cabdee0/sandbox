package com.redhat.service.bridge.shard.operator.resources;

import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.processors.ProcessorDefinition;

public class BridgeExecutorSpec {
    private String image;

    private String id;

    private BridgeDTO bridgeDTO;

    private String processorName;

    private ProcessorDefinition definition;

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

    public ProcessorDefinition getDefinition() {
        return definition;
    }

    public void setDefinition(ProcessorDefinition definition) {
        this.definition = definition;
    }
}
