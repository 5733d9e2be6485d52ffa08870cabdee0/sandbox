package com.redhat.service.smartevents.infra.models.bridges;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.models.gateways.Action;

public class BridgeDefinition {

    @JsonProperty("error_handler")
    private Action errorHandler;

    public BridgeDefinition() {
    }

    public BridgeDefinition(Action errorHandler) {
        this.errorHandler = errorHandler;
    }

    public Action getErrorHandler() {
        return errorHandler;
    }

    public void setErrorHandler(Action errorHandler) {
        this.errorHandler = errorHandler;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BridgeDefinition that = (BridgeDefinition) o;
        return Objects.equals(errorHandler, that.errorHandler);
    }

    @Override
    public int hashCode() {
        return Objects.hash(errorHandler);
    }
}
