package com.redhat.service.smartevents.shard.operator.resources.camel;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// Identical to CamelIntegrationKafkaConnectionFrom but without steps fields
public class CamelIntegrationKafkaConnectionTo {

    String uri;

    Map<String, Object> parameters = new HashMap<>();

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CamelIntegrationKafkaConnectionTo that = (CamelIntegrationKafkaConnectionTo) o;
        return Objects.equals(uri, that.uri) && Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri, parameters);
    }

    @Override
    public String toString() {
        return "CamelIntegrationKafkaConnectionTo{" +
                "uri='" + uri + '\'' +
                ", parameters=" + parameters +
                '}';
    }
}
