package com.redhat.service.smartevents.infra.models.processors;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ProcessorType implements BaseEnumeration {
    SOURCE(Constants.SOURCE_VALUE),
    SINK(Constants.SINK_VALUE),
    ERROR_HANDLER(Constants.ERROR_HANDLER_VALUE);

    final String value;

    ProcessorType(String value) {
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
    public static ProcessorType fromString(String type) {
        return BaseEnumeration.lookup(values(), type);
    }

    public static class Constants {
        public static final String SOURCE_VALUE = "source";
        public static final String SINK_VALUE = "sink";
        public static final String ERROR_HANDLER_VALUE = "error-handler";
    }
}
