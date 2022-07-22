package com.redhat.service.smartevents.infra.models.dto;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.exceptions.BridgeError;

public class ProcessorStatusWrapperDTO {

    @NotNull
    @JsonProperty("processor")
    private ProcessorDTO processor;

    @JsonProperty("exception")
    private BridgeError exception;

    public ProcessorStatusWrapperDTO() {
    }

    public ProcessorStatusWrapperDTO(ProcessorDTO processor) {
        this.processor = processor;
    }

    public ProcessorStatusWrapperDTO(ProcessorDTO processor, BridgeError exception) {
        this.processor = processor;
        this.exception = exception;
    }

    public ProcessorDTO getProcessor() {
        return processor;
    }

    public BridgeError getException() {
        return exception;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProcessorStatusWrapperDTO)) {
            return false;
        }
        ProcessorStatusWrapperDTO that = (ProcessorStatusWrapperDTO) o;
        return getProcessor().equals(that.getProcessor()) && Objects.equals(getException(), that.getException());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getProcessor(), getException());
    }

    @Override
    public String toString() {
        return "ProcessorStatusWrapperDTO{" +
                "processor=" + processor +
                ", exception=" + exception +
                '}';
    }
}
