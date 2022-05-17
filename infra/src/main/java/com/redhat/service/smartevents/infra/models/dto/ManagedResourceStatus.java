package com.redhat.service.smartevents.infra.models.dto;

import java.util.Arrays;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ManagedResourceStatus {
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

    String status;

    // We can not annotate the property `status` directly with `@JsonValue`. See https://issues.redhat.com/browse/MGDOBR-595
    @JsonValue
    public String serialize() {
        return status;
    }

    @SuppressWarnings("unused")
    // Required for JAX-RS deserialisation. See @javax.ws.rs.QueryParam.
    public static ManagedResourceStatus fromString(String status) {
        return Arrays
                .stream(ManagedResourceStatus.values())
                .filter(s -> Objects.equals(s.status, status))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("ManagedResourceStatus '%s' unknown.", status)));
    }

    ManagedResourceStatus(String status) {
        this.status = status;
    }
}
