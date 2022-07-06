package com.redhat.service.smartevents.shard.operator.resources.camel;

import java.util.Objects;

public class CamelIntegrationSpec {

    private CamelIntegrationFlows camelIntegrationFlows;

    public CamelIntegrationFlows getCamelIntegrationFlows() {
        return camelIntegrationFlows;
    }

    public void setCamelIntegrationFlows(CamelIntegrationFlows camelIntegrationFlows) {
        this.camelIntegrationFlows = camelIntegrationFlows;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CamelIntegrationSpec that = (CamelIntegrationSpec) o;
        return Objects.equals(camelIntegrationFlows, that.camelIntegrationFlows);
    }

    @Override
    public int hashCode() {
        return Objects.hash(camelIntegrationFlows);
    }

    @Override
    public String toString() {
        return "CamelIntegrationSpec{" +
                "camelIntegrationFlows=" + camelIntegrationFlows +
                '}';
    }
}
