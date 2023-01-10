package com.redhat.service.smartevents.infra.v1.api.models;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonValue;
import com.redhat.service.smartevents.infra.core.models.BaseEnumeration;
import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;

@Schema(name = "ManagedResourceStatus")
public enum ManagedResourceStatusV1 implements ManagedResourceStatus {
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

    ManagedResourceStatusV1(String value) {
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
    public static ManagedResourceStatusV1 fromString(String type) {
        return BaseEnumeration.lookup(values(), type);
    }

}
