package com.redhat.service.smartevents.shard.operator.resources.camel;

import java.util.List;
import java.util.Objects;

public class CamelIntegrationSpec {

    private List<CamelIntegrationFlow> flows;

    public List<CamelIntegrationFlow> getFlows() {
        return flows;
    }

    public void setFlows(List<CamelIntegrationFlow> flows) {
        this.flows = flows;
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
        return Objects.equals(flows, that.flows);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flows);
    }

    @Override
    public String toString() {
        return "CamelIntegrationSpec{" +
                "flows=" + flows +
                '}';
    }
}
