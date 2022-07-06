package com.redhat.service.smartevents.shard.operator.resources.camel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CamelIntegrationFlows {

    List<CamelIntegrationFrom> camelIntegrationFrom = new ArrayList<>();

    public List<CamelIntegrationFrom> getCamelIntegrationFrom() {
        return camelIntegrationFrom;
    }

    public void setCamelIntegrationFrom(List<CamelIntegrationFrom> camelIntegrationFrom) {
        this.camelIntegrationFrom = camelIntegrationFrom;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CamelIntegrationFlows that = (CamelIntegrationFlows) o;
        return Objects.equals(camelIntegrationFrom, that.camelIntegrationFrom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(camelIntegrationFrom);
    }

    @Override
    public String toString() {
        return "CamelIntegrationFlows{" +
                "camelIntegrationFrom=" + camelIntegrationFrom +
                '}';
    }
}
