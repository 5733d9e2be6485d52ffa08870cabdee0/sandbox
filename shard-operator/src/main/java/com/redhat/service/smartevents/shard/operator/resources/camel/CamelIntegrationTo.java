package com.redhat.service.smartevents.shard.operator.resources.camel;

import java.util.Objects;

public class CamelIntegrationTo {

    private String to;

    public CamelIntegrationTo() {
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CamelIntegrationTo that = (CamelIntegrationTo) o;
        return Objects.equals(to, that.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(to);
    }

    @Override
    public String toString() {
        return "CamelIntegrationTo{" +
                "to='" + to + '\'' +
                '}';
    }
}
