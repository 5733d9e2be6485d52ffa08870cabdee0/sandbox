package com.redhat.service.smartevents.shard.operator.resources.camel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CamelIntegrationFrom {

    String uri;

    Map<String, Object> parameters = new HashMap<>();

    List<CamelIntegrationTo> steps = new ArrayList<>();

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public List<CamelIntegrationTo> getSteps() {
        return steps;
    }

    public void setSteps(List<CamelIntegrationTo> steps) {
        this.steps = steps;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CamelIntegrationFrom that = (CamelIntegrationFrom) o;
        return Objects.equals(uri, that.uri) && Objects.equals(parameters, that.parameters) && Objects.equals(steps, that.steps);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri, parameters, steps);
    }

    @Override
    public String toString() {
        return "CamelIntegrationFrom{" +
                "uri='" + uri + '\'' +
                ", parameters=" + parameters +
                ", steps=" + steps +
                '}';
    }
}
