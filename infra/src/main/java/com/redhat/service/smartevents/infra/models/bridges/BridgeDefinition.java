package com.redhat.service.smartevents.infra.models.bridges;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.models.gateways.Action;

public class BridgeDefinition {

    public static final String ENDPOINT_ERROR_HANDLER_TYPE = "endpoint";

    @JsonProperty("error_handler")
    private Action errorHandler;

    @JsonProperty("resolved_error_handler")
    private Action resolvedErrorHandler;

    public static boolean isEndpointErrorHandlerAction(Action action) {
        return action != null && ENDPOINT_ERROR_HANDLER_TYPE.equals(action.getType());
    }

    public BridgeDefinition() {
    }

    public BridgeDefinition(Action errorHandler, Action resolvedErrorHandler) {
        this.errorHandler = errorHandler;
        this.resolvedErrorHandler = resolvedErrorHandler;
    }

    public boolean hasEndpointErrorHandler() {
        return isEndpointErrorHandlerAction(errorHandler);
    }

    public Action getErrorHandler() {
        return errorHandler;
    }

    public void setErrorHandler(Action errorHandler) {
        this.errorHandler = errorHandler;
    }

    public Action getResolvedErrorHandler() {
        return resolvedErrorHandler;
    }

    public void setResolvedErrorHandler(Action resolvedErrorHandler) {
        this.resolvedErrorHandler = resolvedErrorHandler;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BridgeDefinition that = (BridgeDefinition) o;
        return Objects.equals(errorHandler, that.errorHandler) && Objects.equals(resolvedErrorHandler, that.resolvedErrorHandler);
    }

    @Override
    public int hashCode() {
        return Objects.hash(errorHandler, resolvedErrorHandler);
    }
}
