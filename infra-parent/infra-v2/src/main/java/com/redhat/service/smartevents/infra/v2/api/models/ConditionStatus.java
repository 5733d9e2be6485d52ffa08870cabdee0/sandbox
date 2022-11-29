package com.redhat.service.smartevents.infra.v2.api.models;

import com.fasterxml.jackson.annotation.JsonValue;
import com.redhat.service.smartevents.infra.core.models.BaseEnumeration;

public enum ConditionStatus implements BaseEnumeration {

    TRUE("True"),
    FALSE("False"),
    UNKNOWN("Unknown"),
    FAILED("Failed");

    final String value;

    ConditionStatus(String value) {
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
    public static ConditionStatus fromString(String type) {
        return BaseEnumeration.lookup(values(), type);
    }

}