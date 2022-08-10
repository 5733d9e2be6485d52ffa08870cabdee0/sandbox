package com.redhat.service.smartevents.shard.operator.resources.camel;

import java.util.Objects;

public class CamelIntegrationFlow {

    CamelIntegrationKafkaConnectionFrom from;

    public CamelIntegrationKafkaConnectionFrom getFrom() {
        return from;
    }

    public void setFrom(CamelIntegrationKafkaConnectionFrom from) {
        this.from = from;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CamelIntegrationFlow that = (CamelIntegrationFlow) o;
        return Objects.equals(from, that.from);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from);
    }

    @Override
    public String toString() {
        return "CamelIntegrationFlow{" +
                "from=" + from +
                '}';
    }
}
