package com.redhat.service.smartevents.infra.models;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ManagedResourceStatus implements BaseEnumeration {
    // Creation flow: accepted -> preparing -> provisioning -> ready
    ACCEPTED("accepted"),
    PREPARING("preparing"),
    PROVISIONING("provisioning"),
    READY("ready"),
    // Deleting flow: ready -> deprovision -> deleting -> deleted
    DEPROVISION("deprovision"),
    DELETING("deleting"),
    DELETED("deleted"),
    // When something has gone wrong!
    FAILED("failed");

    final String value;

    ManagedResourceStatus(String value) {
        this.value = value;
    }

    @JsonValue
    @Override
    // We can not annotate the property `value` directly with `@JsonValue`. See https://issues.redhat.com/browse/MGDOBR-595
    public String getValue() {
        return value;
    }

    @SuppressWarnings("unused")
    // Required for JAX-RS deserialisation. See @javax.ws.rs.QueryParam.
    public static ManagedResourceStatus fromString(String type) {
        return BaseEnumeration.lookup(values(), type);
    }

}
